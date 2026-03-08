plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.3.0"
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.4")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.97.0+1.21.4")

    implementation(project(":core"))
    shadow(project(":core"))
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("")
}
