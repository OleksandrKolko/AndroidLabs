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
    }
}

rootProject.name = "AndroidLabs"
include(":lab3:task1")
include(":lab3:task2")
include(":lab4:task1")
include(":lab4:task2")  
include(":lab5:task1")
include(":lab5:task2")
include(":lab5:task3")
include(":lab6:task1")
include(":lab7:task1")
include(":lab8:task1")
include(":lab9:task1")
include(":lab10:task1")
include(":lab13:task1")
include(":lab14:task1")
include(":lab14:task2")