plugins {
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dgs.codegen)
    alias(libs.plugins.flywaydb)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    // Spring
    implementation(libs.springBoot.web)
    implementation(libs.springBoot.validation)
    implementation(libs.springBoot.security)
    implementation(libs.springBoot.jdbc)
    implementation(libs.springBoot.flyway)
    developmentOnly(libs.springBoot.devTools)

    // GraphQL
    implementation(platform(libs.dgs.platformDependencies))
    implementation(libs.dgs.starter)
    implementation(libs.dgs.extendedScalars)

    // Exposed + Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlinDatetime)

    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flywaydb.core)
    implementation(libs.flywaydb.postgresql)

    // Other
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    // Other
    implementation(libs.logback)

    testImplementation(libs.springBoot.test)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("data")) {
        into("data")
    }

    from(rootProject.layout.projectDirectory.dir("assets")) {
        into("static/assets")
    }
}

tasks.generateJava {
    schemaPaths.add("${projectDir}/src/main/resources/schema")
    packageName = "dev.voir.sole.world.graphql.dto"

    language = "KOTLIN"
    generateDocs = true

    generateClient = true
    generateDataTypes = true
    generateKotlinClosureProjections = true

    typeMapping = mutableMapOf(
        "Long" to "kotlin.Long",
        "JSON" to "kotlin.collections.Map<String, Any>",
        "Instant" to "kotlin.time.Instant",
        "LocalDate" to "kotlinx.datetime.LocalDate",
        "Uuid" to "kotlin.uuid.Uuid"
    )
}

tasks.test {
    useJUnitPlatform()
}
