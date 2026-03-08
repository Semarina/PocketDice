plugins {
    id("io.papermc.paper.weight.userdev") version "1.7.1"
    id("com.github.johnrengelman.shadow") version "8.3.0"
}

dependencies {
    implementation(project(":core"))
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("") // Replace the default jar
}
