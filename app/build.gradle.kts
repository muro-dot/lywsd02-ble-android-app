import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// 릴리즈 서명 정보는 저장소에 넣지 않고 ~/.gradle/gradle.properties 에서 읽는다. (docs/RELEASE.md 참고)
val hasReleaseSigning = project.hasProperty("LYWSD02_KEYSTORE")

android {
    namespace = "com.example.lywsd02bledashboard"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.lywsd02bledashboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "1.2.8"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(project.property("LYWSD02_KEYSTORE") as String)
                storePassword = project.property("LYWSD02_STORE_PASSWORD") as String
                keyAlias = project.property("LYWSD02_KEY_ALIAS") as String
                keyPassword = project.property("LYWSD02_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 서명 키가 없는 환경에서는 서명 없이 두어, 디버그 키로 잘못 서명되는 일을 막는다
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

// 릴리즈 빌드를 직접 요청했는데 서명 정보가 없으면 조용히 통과시키지 않고 중단한다. (docs/RELEASE.md 참고)
// 'build' 같은 일반 작업은 영향받지 않도록 명시적으로 요청된 릴리즈 작업만 검사한다.
val releaseTaskPattern = Regex("(^|:)(assemble|bundle|package|install)[A-Za-z]*Release$")
if (gradle.startParameter.taskNames.any { releaseTaskPattern.containsMatchIn(it) }) {
    if (!hasReleaseSigning) {
        throw GradleException("릴리즈 서명 키(LYWSD02_KEYSTORE 등)가 ~/.gradle/gradle.properties 에 없습니다. 릴리즈는 키 보관 PC에서만 빌드하세요. docs/RELEASE.md 참고")
        }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}

// 로컬 환경설정(local.properties) 또는 환경 변수에 배포 경로가 지정된 경우 APK 자동 복사
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    FileInputStream(localPropsFile).use { stream ->
        localProps.load(stream)
    }
}
val deployDir = localProps.getProperty("apk.deploy.dir") ?: System.getenv("APK_DEPLOY_DIR")

if (!deployDir.isNullOrBlank()) {
    val currentVersion = android.defaultConfig.versionName ?: "1.2.8"

    val copyDebugApkToDeployDir = tasks.register<Copy>("copyDebugApkToDeployDir") {
        val apkFolder = layout.buildDirectory.dir("outputs/apk/debug")
        from(apkFolder) {
            include("app-debug.apk")
            rename("app-debug.apk", "lywsd02-ble-android-app-v${currentVersion}.apk")
        }
        from(apkFolder) {
            include("app-debug.apk")
            rename("app-debug.apk", "lywsd02-ble-android-app.apk")
        }
        from(apkFolder) {
            include("app-debug.apk")
            rename("app-debug.apk", "LYWSD02_BLE_Android_App_v${currentVersion}.apk")
        }
        from(apkFolder) {
            include("app-debug.apk")
            rename("app-debug.apk", "LYWSD02_BLE_Android_App.apk")
        }
        from(apkFolder) {
            include("app-debug.apk")
            rename("app-debug.apk", "LYWSD02_BLE_Tool_v${currentVersion}.apk")
        }
        from(apkFolder) {
            include("app-debug.apk")
            rename("app-debug.apk", "LYWSD02_BLE_Tool.apk")
        }
        from(apkFolder) {
            include("app-debug.apk")
        }
        into(file(deployDir))
    }

    val copyReleaseApkToDeployDir = tasks.register<Copy>("copyReleaseApkToDeployDir") {
        val apkFolder = layout.buildDirectory.dir("outputs/apk/release")
        from(apkFolder) {
            include("app-release.apk")
            rename("app-release.apk", "lywsd02-ble-android-app.apk")
        }
        from(apkFolder) {
            include("app-release.apk")
            rename("app-release.apk", "lywsd02-ble-android-app-v${currentVersion}.apk")
        }
        from(apkFolder) {
            include("app-release.apk")
            rename("app-release.apk", "LYWSD02_BLE_Android_App.apk")
        }
        from(apkFolder) {
            include("app-release.apk")
            rename("app-release.apk", "LYWSD02_BLE_Android_App_v${currentVersion}.apk")
        }
        from(apkFolder) {
            include("app-release.apk")
            rename("app-release.apk", "LYWSD02_BLE_Tool.apk")
        }
        from(apkFolder) {
            include("app-release.apk")
            rename("app-release.apk", "LYWSD02_BLE_Tool_v${currentVersion}.apk")
        }
        from(apkFolder) {
            include("app-release.apk")
        }
        into(file(deployDir))
    }

    afterEvaluate {
        tasks.named("assembleDebug").configure {
            finalizedBy(copyDebugApkToDeployDir)
        }
        tasks.named("assembleRelease").configure {
            finalizedBy(copyReleaseApkToDeployDir)
        }
    }
}

