plugins {
    java
}

subprojects {
    apply(plugin = "java")

    group = "me.sepehrhn"
    version = "0.2.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    }

    tasks.test {
        useJUnitPlatform()
    }
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
