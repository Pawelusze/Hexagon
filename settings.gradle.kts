pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
        maven("https://repo.panda-lang.org/releases") {
            name = "panda"
        }
        maven("https://maven.enginehub.org/repo/") {
            name = "enginehub"
        }
    }
}

rootProject.name = "hexagon"

include("api", "plugin", "benchmark")
project(":api").name = "hexagon-api"
project(":plugin").name = "hexagon-plugin"
project(":benchmark").name = "hexagon-benchmark"
