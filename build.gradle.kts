plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val mavenDirectory: String = if (System.getenv("MAVEN_DIR") == null) {
    "$projectDir/repo"
} else System.getenv("MAVEN_DIR")
val jdDirectory: String? = if (System.getenv("JAVADOCS_DIR") == null) {
    null
} else System.getenv("JAVADOCS_DIR")

// different version convention than Nyaa plugins
group = "de.greensurvivors"
version = buildString {
    append(getProperty("plugin_version"))

    if (getProperty("is_release").toBoolean().not()) {
        append("-Snapshot")
    }

    append("+${getProperty("minecraft_version")}")

    if (System.getenv("BUILD_NUMBER") != null) {
        append("+${System.getenv("BUILD_NUMBER")}")
    }
}
description = "Padlock is a chest protection plugin for Paper." +
        " It is 80% compatible with original Lockette," +
        " but delivers a lot of performance enhancements and feature options."

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(getProperty("java_version").toInt()))

    withSourcesJar()
    //withJavadocJar()
}

repositories {
    mavenCentral()
    maven {
        name = "Paper"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven { //worldguard
        name = "sk89q-repo"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven { // vault
        url = uri("https://jitpack.io")
    }
}

dependencies {
    paperweight.paperDevBundle("${getProperty("minecraft_version")}.build.+")

    implementation("org.apache.commons:commons-collections4:${getProperty("commons_collections_version")}")
    compileOnly("com.github.ben-manes.caffeine:caffeine:${getProperty("caffeine_version")}") // caches
    implementation("de.mkammerer:argon2-jvm:${getProperty("argon_version")}") // native password hashing with argon2

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:${getProperty("worldguard_compileVersion")}") {
        isTransitive = false
    }
    compileOnly("com.github.MilkBowl:VaultAPI:${getProperty("vault_version")}") {
        isTransitive = false
    }
}

tasks {
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        expand(
            providers.gradlePropertiesPrefixedBy("")
                .get()
                .toMutableMap() // f you gradle for being inconvenient in newer versions
                .plus("version" to version)
                .plus("description" to description)
        )
    }

    compileJava {
        // extra compile warnings
        // options.compilerArgs += ["-Xlint:deprecation"]
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(getProperty("java_version").toInt())
    }

    runServer {
        downloadPlugins {
            // make sure to double-check the version id on the Modrinth version page
            modrinth("worldedit", getProperty("worldEdit_runVersion"))
            modrinth("worldguard", getProperty("worldGuard_runVersion"))
        }

        // disable bstats, as it isn't needed for dev environment
        doFirst { // this happens after downloading the plugins above, but before the server starts
            val cfg = runDirectory.get().asFile.resolve("plugins/bStats/config.yml")
            if (!cfg.exists()) {
                cfg.parentFile.mkdirs()
                cfg.createNewFile()
            }
            cfg.writeText("enabled: false\n")
        }
        // automatically agree to eula
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}

publishing {
    publications {
        create<MavenPublication>("Padlock") {
            from(components["java"])
            artifactId = "Padlock"
            version = buildString { // don't include mc version nor build number here
                append(getProperty("plugin_version"))

                if (getProperty("is_release").toBoolean().not()) {
                    append("-Snapshot")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(mavenDirectory)
        }
    }
}

private fun getProperty(value: String): String = providers.gradleProperty(value).get()
