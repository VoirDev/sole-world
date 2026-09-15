import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dgs.codegen)
    alias(libs.plugins.openapi.generator)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    constraints {
        // Taken ahead of the version Spring Boot pins, because the release image scan gates on
        // criticals and these three are exactly that. Remove once Boot's own pin catches up.
        val tomcatFixes = "CVE-2026-65182, CVE-2026-65905, CVE-2026-68525"
        implementation(libs.tomcat.embedCore) { because(tomcatFixes) }
        implementation(libs.tomcat.embedEl) { because(tomcatFixes) }
        implementation(libs.tomcat.embedWebsocket) { because(tomcatFixes) }
    }

    // Spring
    implementation(libs.springBoot.web)
    implementation(libs.springBoot.validation)
    developmentOnly(libs.springBoot.devTools)

    // GraphQL
    implementation(platform(libs.dgs.platformDependencies))
    implementation(libs.dgs.starter)

    // Kotlin
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.springBoot.test)
}

/**
 * Downloads the developer consoles' third-party assets and verifies them against pinned digests.
 *
 * The consoles used to fetch their JavaScript from a CDN at runtime, which put a third party inside
 * the trust boundary of a route that is public by default and left both consoles blank on an
 * air-gapped deployment. Vendoring them here means the image carries exactly the reviewed bytes, and
 * a changed upstream file fails the build instead of shipping quietly.
 */
abstract class VendorConsoleAssets : DefaultTask() {
    /** Pinned assets, one `<path> <url> <sha256>` per line; `#` starts a comment. */
    @get:InputFile
    abstract val manifest: RegularFileProperty

    /** Directory the verified assets are written to, laid out by their manifest path. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun vendor() {
        val root = outputDirectory.get().asFile
        root.deleteRecursively()

        manifest.get().asFile.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val (path, url, expectedDigest) = line.split(Regex("\\s+"), limit = 3).also {
                    require(it.size == 3) { "Malformed asset line: $line" }
                }

                val bytes = URI(url).toURL().readBytes()
                val actualDigest = MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }

                check(actualDigest == expectedDigest) {
                    "$url does not match its pinned digest.\n" +
                        "  expected $expectedDigest\n" +
                        "  actual   $actualDigest\n" +
                        "Review the change before updating api/console-assets.txt."
                }

                val target = root.resolve(path)
                target.parentFile.mkdirs()
                target.writeBytes(bytes)
                logger.lifecycle("Vendored $path (${bytes.size} bytes)")
            }
    }
}

val vendorConsoleAssets = tasks.register<VendorConsoleAssets>("vendorConsoleAssets") {
    description = "Downloads and verifies the developer consoles' third-party assets."
    manifest = layout.projectDirectory.file("console-assets.txt")
    outputDirectory = layout.buildDirectory.dir("console-assets")
}

// Docker layers are only useful if what changes often is separated from what does not. Spring Boot's
// default split puts this project's classes and the 40 MB bundled dataset in one layer, so a
// one-line code change reships the dataset too. The dataset gets a layer of its own.
tasks.bootJar {
    layered {
        application {
            intoLayer("spring-boot-loader") { include("org/springframework/boot/loader/**") }
            // Patterns for application content are relative to BOOT-INF/classes/.
            intoLayer("dataset") { include("data/**") }
            intoLayer("application")
        }
        dependencies {
            intoLayer("snapshot-dependencies") { include("*:*:*SNAPSHOT") }
            intoLayer("dependencies")
        }
        // Least to most likely to change.
        layerOrder = listOf(
            "dependencies",
            "spring-boot-loader",
            "snapshot-dependencies",
            "dataset",
            "application",
        )
    }
}

// The 9,773 image files under assets/ are not on the classpath. They are 90 MB that never changes
// between releases, and putting them in the jar meant a one-line code change rebuilt and reshipped
// all of it. The image copies them as their own Docker layer instead; see api/Dockerfile.
tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("data")) {
        into("data")
    }

    // Held outside the static resource path so that switching a console off leaves nothing behind
    // for the static resource handler to serve.
    from(vendorConsoleAssets) {
        into("console/assets")
    }
}

tasks.generateJava {
    schemaPaths.add("$projectDir/src/main/resources/schema")
    packageName = "dev.voir.sole.world.graphql.dto"

    language = "KOTLIN"
    generateDocs = true

    generateClient = true
    generateDataTypes = true
    generateKotlinClosureProjections = true

    // The schema declares one scalar; see GraphQLScalarConfig.
    typeMapping = mutableMapOf("LocalDate" to "kotlinx.datetime.LocalDate")
}

openApiGenerate {
    generatorName = "kotlin-spring"
    inputSpec = layout.projectDirectory.file("src/main/resources/openapi/openapi.yaml")
    outputDir = layout.buildDirectory.dir("generated/openapi")
    // Regenerate from scratch so a removed path cannot leave a stale interface behind.
    cleanupOutput = true
    apiPackage = "dev.voir.sole.world.openapi.api"
    modelPackage = "dev.voir.sole.world.openapi.model"

    configOptions = mapOf(
        // Only the contract is generated; controllers implement the interfaces by hand.
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "serializationLibrary" to "jackson",
        "useTags" to "true",
        "enumPropertyNaming" to "UPPERCASE",
        "skipDefaultInterface" to "true",
    )

    typeMappings = mapOf("DateTime" to "java.time.Instant")

    // Relationships a caller did not ask for are absent rather than null, so a response carries only
    // what was requested. Fully qualified because the generator does not add imports for these.
    additionalProperties = mapOf(
        "additionalModelTypeAnnotations" to
            "@com.fasterxml.jackson.annotation.JsonInclude(" +
            "com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)",
    )
}

sourceSets.main {
    kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

// ktlint walks the main source set, which includes the contract types both generators produce, so
// it has to wait for them. What they contain is not checked -- .editorconfig switches ktlint off
// for everything under build/ -- but the files still have to exist before the task reads the tree.
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
    dependsOn(tasks.openApiGenerate, tasks.generateJava)
}

tasks.test {
    useJUnitPlatform()
}
