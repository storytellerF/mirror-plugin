plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.storytellerf"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("mirrorSettings") {
            id = "com.storytellerf.mirror"
            implementationClass = "com.storytellerf.mirror.MirrorSettingsPlugin"
            displayName = "Maven Mirror Settings Plugin"
            description = "Adds a globally selected Maven mirror to dependency resolution repositories."
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(kotlin("test"))
}
