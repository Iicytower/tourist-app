pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WanderList"

include(":app")
include(":core")
include(":domain")
include(":data")
include(":feature-search")
include(":feature-map")
include(":feature-mylist")
include(":feature-assistant")
include(":feature-settings")
include(":feature-detail")
