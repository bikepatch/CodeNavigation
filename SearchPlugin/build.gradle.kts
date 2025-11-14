import org.jetbrains.intellij.IntelliJPluginExtension

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.example.occurrence"
version = "1.0.0"

repositories {
    mavenCentral()
}

configure<IntelliJPluginExtension> {
    version.set("2023.3")
    type.set("IC")
    plugins.set(listOf())
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks {

    buildPlugin {
    }

    runIde {
        jvmArgs("-Xmx2G")
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}