plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSpring) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dgs.codegen) apply false
    alias(libs.plugins.openapi.generator) apply false
}

// VERSION is the one place the release version lives: Prepare Release bumps it, and Publish Release
// publishes on a change to it. `-PreleaseVersion` overrides it for a one-off local build only.
val projectVersion = providers.gradleProperty("releaseVersion")
    .orElse(providers.fileContents(layout.projectDirectory.file("VERSION")).asText.map { it.trim() })
    .get()

group = "dev.voir.sole.world"
version = projectVersion

subprojects {
    // Every project carries the release version, so the API jar's manifest records it
    // (Implementation-Version) and the application logs it at startup.
    version = rootProject.version

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
    }
}
