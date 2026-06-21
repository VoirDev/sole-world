plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSpring) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dgs.codegen) apply false
    alias(libs.plugins.flywaydb) apply false
}

val projectVersion = providers.gradleProperty("releaseVersion").orElse("1.0.1").get()

group = "dev.voir.sole"
version = projectVersion

subprojects {
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
