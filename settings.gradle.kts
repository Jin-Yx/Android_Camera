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
        maven(url = "https://mirrors.huaweicloud.com/repository/maven")
        google()
        mavenCentral()
    }
}

include(":app")
include(listOf(":libcamera-v4l2", ":libcamera-sys"))
 