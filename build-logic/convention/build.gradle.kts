import org.jetbrains.kotlin.config.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "mikle.sam.moex.buildlogic"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        //jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Only needed to compile against KotlinCompile task types
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("kotlinJvm18") {
            id = "moex.kotlin.jvm18"
            implementationClass = "mikle.sam.moex.buildlogic.KotlinJvm18ConventionPlugin"
        }
    }
}