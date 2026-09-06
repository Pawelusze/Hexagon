plugins {
    id("hexagon.java-conventions")
    alias(libs.plugins.run.paper)
}

description = "Hexagon plugin runtime for Paper."

// Libraries Paper downloads at startup, listed for HexagonLoader by the libraryList task below.
val runtimeLibrary: Configuration by configurations.creating

configurations.compileOnly { extendsFrom(runtimeLibrary) }

configurations.testImplementation { extendsFrom(runtimeLibrary) }

dependencies {
    implementation(project(":hexagon-api"))
    compileOnly(libs.paper.api)
    compileOnly(libs.luckperms.api)
    compileOnly(libs.worldedit.bukkit)
    compileOnly(libs.jetbrains.annotations)

    runtimeLibrary(libs.configurate.yaml)
    runtimeLibrary(libs.litecommands.bukkit)
    runtimeLibrary(libs.litecommands.adventure)

    testImplementation(libs.paper.api)
}

val libraryList by tasks.registering {
    val coordinates = runtimeLibrary.dependencies.map { "${it.group}:${it.name}:${it.version}" }
    val listFile = layout.buildDirectory.file("generated/libraries.list")
    inputs.property("coordinates", coordinates)
    outputs.file(listFile)
    doLast { listFile.get().asFile.writeText(coordinates.joinToString(separator = "\n", postfix = "\n")) }
}

tasks {
    processResources {
        val pluginVersion = mapOf("version" to project.version)
        inputs.properties(pluginVersion)
        filesMatching("paper-plugin.yml") { expand(pluginVersion) }
        from(libraryList)
    }

    jar {
        archiveBaseName = "Hexagon"
        from(
            project(":hexagon-api")
                .sourceSets.main
                .get()
                .output,
        )
    }

    runServer {
        minecraftVersion("1.21.9")
        downloadPlugins {
            // Selections come from WorldEdit, so the test server needs it. Modrinth publishes one
            // version number per loader, hence the direct link to the Bukkit build.
            url("https://cdn.modrinth.com/data/1u6JkXh5/versions/F5ea2ov3/worldedit-bukkit-7.4.5.jar")
        }
    }
}
