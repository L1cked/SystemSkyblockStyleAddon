import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.compile.JavaCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "systemSkyblockStyleAddon"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenLocal()
    flatDir { dirs("libs") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-jvm-default=enable",
            "-Xjsr305=strict"
        )
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly(fileTree("../MayorSystem/build/libs") { include("MayorSystem-*.jar") })
    val mayorSystemJar = file("libs/MayorSystem.jar")
    if (mayorSystemJar.exists()) {
        compileOnly(files(mayorSystemJar))
    }

    implementation(kotlin("stdlib"))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version.toString())
    }
}

tasks.withType<ShadowJar>().configureEach {
    // Keep the shadow jar for local testing, but don't overwrite the thin jar.
    archiveClassifier.set("shadow")
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
