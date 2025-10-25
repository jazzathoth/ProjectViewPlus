plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "com.jazzathoth"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    intellijPlatform {
        create("IC", "2025.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.jazzathoth.project-tree-plugin"
        name = "Project Tree"
        version = project.version.toString()
        description = "Custom Project View with manual reordering and persistance via JSON"
        changeNotes = "Alpha: context menu, drag and drop reorder, file system auto refresh with vfs"

        ideaVersion {
            sinceBuild = "242"
        }

        vendor {
            name = "jazzathoth"
            email = "38414120+jazzathoth@users.noreply.github.com"
        }
    }

}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
