import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.vanniktech.maven.publish)
    kotlin("native.cocoapods")
}

group = "com.shepeliev"

version = System.getenv("VERSION") ?: "0.0.0"

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    cocoapods {
        version = project.version.toString()
        summary = "WebRTC Kotlin Multiplatform SDK"
        homepage = "https://github.com/shepeliev/webrtc-kmp"
        ios.deploymentTarget = "13.0"

        noPodspec()

        pod("WebRTC-SDK") {
            version =
                libs.versions.webrtc.ios.sdk
                    .get()
            moduleName = "WebRTC"
            packageName = "WebRTC"
        }
    }

    android {
        namespace = "com.shepeliev.webrtckmp"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

        withDeviceTestBuilder {
            sourceSetTreeName = KotlinSourceSetTree.test.name
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js {
        useCommonJs()
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        // Serve sources to debug inside browser
                        static(project.rootDir.path)
                    }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jsAndWasmJs") {
                withJs()
                withWasmJs()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.coroutines)
        }

        androidMain.dependencies {
            api(libs.webrtc.sdk)
            implementation(libs.kotlin.coroutines.android)
            implementation(libs.androidx.coreKtx)
            implementation(libs.androidx.startup)
        }

        jsMain.dependencies {
            implementation(npm("webrtc-adapter", "9.0.6"))
        }

        wasmJsMain.dependencies {
            implementation(libs.kotlin.browser)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-annotations-common"))
            implementation(libs.kotlin.coroutines.test)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.rules)
        }

        val iosX64AndSimulatorArm64Main =
            create("iosX64AndSimulatorArm64Main") {
                dependsOn(iosMain.get())
            }
        iosX64Main.get().dependsOn(iosX64AndSimulatorArm64Main)
        iosSimulatorArm64Main.get().dependsOn(iosX64AndSimulatorArm64Main)
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(project.group.toString(), project.name, "${project.version}")

    pom {
        name = "WebRTC KMP"
        description = "WebRTC Kotlin Multiplatform SDK."
        url = "https://github.com/shepeliev/webrtc-kmp"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "shepeliev"
                name = "Oleksandr Shepeliev"
                email = "a.shepeliev@gmail.com"
            }
        }

        scm {
            url = "https://github.com/shepeliev/webrtc-kmp"
            connection = "scm:git:https://github.com/shepeliev/webrtc-kmp.git"
            developerConnection = "scm:git:https://github.com/shepeliev/webrtc-kmp.git"
        }
    }
}
