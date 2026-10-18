import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// 릴리즈 서명 정보는 저장소에 넣지 않고 ~/.gradle/gradle.properties 에서 읽는다. (docs/RELEASE.md 참고)
val hasReleaseSigning = project.hasProperty("LYWSD02_KEYSTORE")

// 키 로테이션용 정보: v1.2.8 이하는 디버그 키로 서명돼 배포됐다.
// 정식 키만으로 서명하면 기존 설치본이 업데이트를 거부하므로, 예전 키 + 위임장(lineage)이 함께 있어야 릴리즈를 허용한다.
val rotationPropertyNames = listOf(
    "LYWSD02_LEGACY_KEYSTORE", "LYWSD02_LEGACY_KEY_ALIAS", "LYWSD02_LEGACY_STORE_PASSWORD", "LYWSD02_LINEAGE"
)
val hasRotationSigning = hasReleaseSigning && rotationPropertyNames.all { project.hasProperty(it) }

android {
    namespace = "com.example.lywsd02bledashboard"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.lywsd02bledashboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "1.2.9"
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
    if (!hasRotationSigning) {
        throw GradleException("키 로테이션 정보(${rotationPropertyNames.joinToString()})가 ~/.gradle/gradle.properties 에 없습니다. 이게 없으면 기존 사용자가 업데이트할 수 없습니다. docs/RELEASE.md 참고")
    }
}

// ── 키 로테이션 재서명 ──
// AGP가 만든 app-release.apk(정식 키 단독 서명)는 기존 설치본 위에 설치되지 않는다.
// 그래서 apksigner로 다시 서명한다: v2 = 예전 디버그 키(Android 7~8용), v3 = 정식 키 + lineage(Android 9+용).
// 배포할 파일은 이 태스크가 만든 outputs/apk/release-rotated/ 의 APK뿐이다.

// 지문이 다르면 키 파일이 바뀐 것이므로 배포하면 기존 사용자 업데이트가 깨진다 → 빌드를 멈춘다.
val legacyCertSha256 = "4c2ab9cb4c8634be5b345105365037414f5d05f91645bb3ebed43d2d59541faa" // v1.0.0~v1.2.8 디버그 키
val releaseCertSha256 = "4780510f53422fa38c4e06822cc929f55fd8c312c77d2864d3eda3d7fa237829" // 정식 키 (CN=lywsd02)
// 로테이션된 키(v3)를 인식하는 첫 안드로이드 버전(9 = API 28). 그 아래는 예전 키 서명을 본다.
val rotationMinSdk = 28

val rotatedApkDir = layout.buildDirectory.dir("outputs/apk/release-rotated")
val rotatedApkName = "lywsd02-ble-android-app-v${android.defaultConfig.versionName}.apk"
val sdkDirectoryProvider = androidComponents.sdkComponents.sdkDirectory

val signReleaseWithLineage = tasks.register("signReleaseWithLineage") {
    description = "릴리즈 APK를 예전 키 + 정식 키 + lineage로 재서명하고 지문을 검증한다"
    // 실패한 빌드의 예전 APK를 재서명하지 않도록, assembleRelease가 성공해야만 실행되게 한다
    dependsOn("assembleRelease")

    // 설정 캐시 호환: 실행 단계에서 project를 참조하지 않도록 값만 미리 꺼내 둔다
    val readProperty = { name: String -> (project.findProperty(name) as String?).orEmpty() }
    val newKeystore = readProperty("LYWSD02_KEYSTORE")
    val newAlias = readProperty("LYWSD02_KEY_ALIAS")
    val newStorePassword = readProperty("LYWSD02_STORE_PASSWORD")
    val newKeyPassword = readProperty("LYWSD02_KEY_PASSWORD")
    val legacyKeystore = readProperty("LYWSD02_LEGACY_KEYSTORE")
    val legacyAlias = readProperty("LYWSD02_LEGACY_KEY_ALIAS")
    val legacyPassword = readProperty("LYWSD02_LEGACY_STORE_PASSWORD")
    val lineageFile = readProperty("LYWSD02_LINEAGE")
    val inputApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
    val outputDir = rotatedApkDir.get().asFile
    val outputApkName = rotatedApkName
    val sdkDir = sdkDirectoryProvider.map { it.asFile }
    val expectedLegacy = legacyCertSha256
    val expectedRelease = releaseCertSha256
    val minSdkForRotation = rotationMinSdk.toString()

    doLast {
        // 가장 최신 build-tools의 apksigner를 쓴다 (lineage·rotation 옵션 지원 필요)
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val apksignerName = if (isWindows) "apksigner.bat" else "apksigner"
        val apksigner = sdkDir.get().resolve("build-tools").listFiles()
            ?.filter { it.resolve(apksignerName).exists() }
            ?.maxByOrNull { it.name }
            ?.resolve(apksignerName)
            ?: throw GradleException("SDK build-tools 에서 apksigner 를 찾을 수 없습니다")

        if (!inputApk.exists()) throw GradleException("재서명할 APK가 없습니다: $inputApk")
        listOf(legacyKeystore, lineageFile, newKeystore).forEach {
            if (!File(it).exists()) throw GradleException("서명 파일이 없습니다: $it (백업: \\\\192.168.0.250\\iso\\안드로이드키)")
        }

        // 비밀번호는 명령줄(작업 관리자에 보임) 대신 환경 변수로만 넘긴다
        val secretEnv = mapOf(
            "LYWSD02_OLD_PASS" to legacyPassword,
            "LYWSD02_NEW_STORE_PASS" to newStorePassword,
            "LYWSD02_NEW_KEY_PASS" to newKeyPassword
        )
        val runApksigner = { args: List<String> ->
            val process = ProcessBuilder(listOf(apksigner.absolutePath) + args)
                .redirectErrorStream(true)
                .apply { environment().putAll(secretEnv) }
                .start()
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() != 0) throw GradleException("apksigner 실패:\n$output")
            output
        }

        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val outputApk = outputDir.resolve(outputApkName)

        runApksigner(
            listOf(
                "sign",
                "--ks", legacyKeystore, "--ks-key-alias", legacyAlias,
                "--ks-pass", "env:LYWSD02_OLD_PASS", "--key-pass", "env:LYWSD02_OLD_PASS",
                "--next-signer",
                "--ks", newKeystore, "--ks-key-alias", newAlias,
                "--ks-pass", "env:LYWSD02_NEW_STORE_PASS", "--key-pass", "env:LYWSD02_NEW_KEY_PASS",
                "--lineage", lineageFile,
                "--rotation-min-sdk-version", minSdkForRotation,
                "--v4-signing-enabled", "false",
                "--out", outputApk.absolutePath,
                inputApk.absolutePath
            )
        )

        // 검증: Android 7~8은 예전 키, Android 9+는 정식 키로 보여야 한다. 하나라도 틀리면 결과물을 지운다.
        val oldPlatformCerts = runApksigner(listOf("verify", "--print-certs", "--max-sdk-version", "27", outputApk.absolutePath))
        val newPlatformCerts = runApksigner(listOf("verify", "--print-certs", "--min-sdk-version", minSdkForRotation, outputApk.absolutePath))
        val lineageCerts = runApksigner(listOf("lineage", "--in", outputApk.absolutePath, "--print-certs"))
        val problems = buildList {
            if (!oldPlatformCerts.contains(expectedLegacy)) add("Android 7~8 서명이 예전 디버그 키가 아님")
            if (!newPlatformCerts.contains(expectedRelease)) add("Android 9+ 서명이 정식 키가 아님")
            if (!lineageCerts.contains(expectedLegacy) || !lineageCerts.contains(expectedRelease)) add("lineage에 두 키가 모두 들어있지 않음")
        }
        if (problems.isNotEmpty()) {
            outputDir.deleteRecursively()
            throw GradleException("재서명 검증 실패: ${problems.joinToString()}")
        }
        logger.lifecycle("키 로테이션 서명 완료: $outputApk")
    }
}

// assembleRelease만 실행해도 재서명이 자동으로 따라오게 한다
tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(signReleaseWithLineage)
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

    // 릴리즈는 키 로테이션 재서명본만 배포한다. app-release.apk(정식 키 단독)는 기존 사용자에게 설치되지 않으므로 복사하지 않는다.
    val copyReleaseApkToDeployDir = tasks.register<Copy>("copyReleaseApkToDeployDir") {
        dependsOn(signReleaseWithLineage)
        val signedApkName = rotatedApkName
        val releaseFileNames = listOf(
            "lywsd02-ble-android-app.apk",
            "lywsd02-ble-android-app-v${currentVersion}.apk",
            "LYWSD02_BLE_Android_App.apk",
            "LYWSD02_BLE_Android_App_v${currentVersion}.apk",
            "LYWSD02_BLE_Tool.apk",
            "LYWSD02_BLE_Tool_v${currentVersion}.apk"
        )
        releaseFileNames.forEach { targetName ->
            from(rotatedApkDir) {
                include(signedApkName)
                rename(signedApkName, targetName)
            }
        }
        into(file(deployDir))
    }

    afterEvaluate {
        tasks.named("assembleDebug").configure {
            finalizedBy(copyDebugApkToDeployDir)
        }
    }
    signReleaseWithLineage.configure {
        finalizedBy(copyReleaseApkToDeployDir)
    }
}

