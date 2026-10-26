import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    kotlin("native.cocoapods")
}

kotlin {
    cocoapods {
        version = "1.0"
        summary = "Compose app"
        homepage = "not published"
        ios.deploymentTarget = "13.0"

        pod("WebRTC-SDK") {
            version = libs.versions.webrtc.ios.sdk.get()
            moduleName = "WebRTC"
            packageName = "WebRTC"
            linkOnly = true
        }

        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "ComposeApp"
            isStatic = true
            export(project(":webrtc-kmp"))
        }

        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }

    android {
        namespace = "com.shepeliev.webrtckmp.sample.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.sampleMinSdk.get().toInt()
        androidResources { enable = true }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    js {
        browser {
            binaries.executable()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    // Serve sources to debug inside browser
                    static(project.projectDir.path)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlin.coroutines)
            implementation(libs.kermit)
            api(project(":webrtc-kmp"))
        }

        androidMain.dependencies {
            implementation(libs.kotlin.coroutines.android)
            implementation(libs.androidx.coreKtx)
            implementation(libs.accompanist.permissions)
        }

        jsMain.dependencies {
            implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
            implementation(libs.kotlin.wrappers.mui)
            implementation(libs.kotlin.wrappers.react)
            implementation(libs.kotlin.wrappers.reactDom)
            implementation(libs.kotlin.wrappers.emotion)
            implementation(libs.kotlin.wrappers.emotion.styled)
        }
    }
}
