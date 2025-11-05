rootProject.name = "MagicSpellsParent"

include("core")
include("factions")
include("memory")
include("shop")
include("teams")
include("towny")

include(":nms:shared")
include(":nms:latest")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
