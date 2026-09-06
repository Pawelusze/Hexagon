plugins {
    id("hexagon.java-conventions")
    alias(libs.plugins.run.paper)
}

description = "A plugin that times Hexagon against WorldGuard on a running server."

dependencies {
    compileOnly(project(":hexagon-api"))
    compileOnly(libs.paper.api)
    compileOnly(libs.worldedit.bukkit)
    // WorldGuard pins older Guava and Gson than WorldEdit does; only its own classes are needed here.
    compileOnly(libs.worldguard.bukkit) { isTransitive = false }
    compileOnly(libs.worldguard.core) { isTransitive = false }
}

tasks {
    jar {
        archiveBaseName = "HexagonBenchmark"
    }

    runServer {
        minecraftVersion("1.21.9")
        pluginJars(project(":hexagon-plugin").tasks.named<Jar>("jar").flatMap { it.archiveFile })
        downloadPlugins {
            url("https://cdn.modrinth.com/data/1u6JkXh5/versions/F5ea2ov3/worldedit-bukkit-7.4.5.jar")
            url("https://cdn.modrinth.com/data/DKY9btbd/versions/PO4MKx7e/worldguard-bukkit-7.0.14-dist.jar")
        }
    }
}
