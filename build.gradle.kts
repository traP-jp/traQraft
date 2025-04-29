import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "jp.trap"
version = "1.0"
description = "connect Minecraft with traQ"

val apiVersion = "1.21.4"
val author = "TwoSquirrels"
val website = "https://github.com/traP-jp/traQraft"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    runServer {
        minecraftVersion(apiVersion)
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "apiVersion" to apiVersion,
        "author" to author,
        "description" to description,
        "website" to website,
        "kotlinVersion" to getKotlinPluginVersion()
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
