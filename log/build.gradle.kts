import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.binary.validator.plugin)
    alias(libs.plugins.vanniktech.publication)
}

private val loggerJvmTarget = JvmTarget.JVM_11

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate()

    jvm {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(loggerJvmTarget)
                }
            }
        }
    }

    androidLibrary {
        namespace = "software.amazon.app.kmplogger"
        compileSdk = 36
        minSdk = 27

        compilerOptions {
            jvmTarget.set(loggerJvmTarget)
        }
    }

    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
    val macosTargets = listOf(macosArm64(), macosX64())

    (iosTargets + macosTargets).forEach {
        it.compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-Xadd-light-debug=enable",
                "-Xexport-kdoc",
            )
        }
    }

    // Frameworks are only needed for Xcode/iOS app consumption.
    iosTargets.forEach {
        it.binaries.framework {
            baseName = "KMPLogger"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("public/src/commonMain/kotlin")
            dependencies {
                project.dependencies.platform(libs.kotlin.bom)
                implementation(libs.atomicfu)
                implementation(libs.kotlin.coroutines.core)
            }
        }
        commonTest {
            kotlin.srcDir("public/src/commonTest/kotlin")
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.assert.k)
                implementation(libs.kotlin.coroutines.test)
            }
        }
        jvmMain {
            kotlin.srcDir("public/src/jvmMain/kotlin")
        }
        jvmTest {
            kotlin.srcDir("public/src/jvmTest/kotlin")
        }
        androidMain {
            kotlin.srcDir("public/src/androidMain/kotlin")
        }
        appleMain {
            kotlin.srcDir("public/src/appleMain/kotlin")
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            sourcesJar = true,
        ),
    )
    coordinates(
        artifactId = "kmp-logger-$name",
    )
}

// Testing Configuration
tasks.withType(Test::class) {
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Code Quality Configuration
detekt {
    config.from(files("../gradle/detekt-config.yml"))
    buildUponDefaultConfig = true
    parallel = true
}

// Release Task
tasks.register("release").configure {
    dependsOn("ktlintCheck", "detekt", "allTests", "build")
}
