plugins {
    `java-library`
}

dependencies {
    // Core logic only (no Bukkit/Fabric dependencies if possible, or only compileOnly abstractions)
    compileOnly("net.kyori:adventure-api:4.17.0") // Needed for shared text handling?
}
