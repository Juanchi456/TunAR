pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositorio oficial de TarsosDSP (no está publicado en Maven Central).
        // Se restringe al grupo be.tarsos.dsp para no resolver otras dependencias desde acá.
        exclusiveContent {
            forRepository {
                maven {
                    name = "TarsosDSP"
                    url = uri("https://mvn.0110.be/releases")
                }
            }
            filter { includeGroup("be.tarsos.dsp") }
        }
    }
}

rootProject.name = "TunAR"
include(":app")
 