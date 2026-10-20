import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

/**
 * 로컬 서버(`./gradlew :server:runLocal`, `:server:startLocalServer`)의 주소. 기능 테스트와 실화면 흐름이 기본으로 붙는다.
 * 포트는 서버 빌드와 같은 `-PlocalServerPort`(기본 8095)를 본다.
 */
val localServerUrl = "http://localhost:${providers.gradleProperty("localServerPort").orNull ?: "8095"}"

/** 실화면 흐름을 돌리는 실행인지. 이때는 앱을 로컬 서버 주소로 빌드한다. */
val requestedTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val e2eRequested = requestedTasks.any { it == "verifyAll" || it == "e2eAndroid" || it == "e2eIos" }

/**
 * 서버 주소를 `local.properties` 의 `server.baseUrl` 에서 읽는다.
 *
 * 비워 두면 서버 없이 기기 안 시연용 데이터만으로 도는 로컬 모드로 빌드된다. 값을 넣으면 그 서버에 붙는다.
 *   - Android 에뮬레이터: http://10.0.2.2:8095 (`adb reverse tcp:8095 tcp:8095` 을 했으면 http://localhost:8095)
 *   - iOS 시뮬레이터:     http://localhost:8095
 *
 * 환경변수 `ODS_SERVER_BASE_URL` 이 있으면 그 값이 먼저다. `local.properties` 를 고치지 않고 서버 모드로 빌드할 때 쓴다.
 * iOS 실화면 흐름은 `xcodebuild ... ODS_SERVER_BASE_URL=<주소>` 로 넘겨 Xcode 빌드 단계가 부르는 Gradle 이 이 값을 본다.
 * 실화면 흐름(`e2eAndroid`·`e2eIos`·`verifyAll`)을 돌릴 때는 따로 주지 않아도 로컬 서버 주소다 —
 * Android 는 `.maestro/run.sh` 가 `adb reverse` 로 에뮬레이터의 localhost 를 이 PC 로 잇는다.
 */
val serverBaseUrlProperty: String = providers.environmentVariable("ODS_SERVER_BASE_URL").orNull?.trim()
    ?: if (e2eRequested) localServerUrl else localProperties.getProperty("server.baseUrl").orEmpty().trim()

/**
 * configuration cache 가 켜져 있어 빌드 스크립트 객체를 붙잡는 doLast 람다는 쓸 수 없다.
 * 입력과 출력을 프로퍼티로 선언한 타입 태스크로 만든다.
 */
abstract class GenerateServerConfigTask : DefaultTask() {
    @get:Input
    abstract val serverBaseUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val directory = outputDir.get().asFile.resolve("com/eunilsung/ods/data/remote/server")
        directory.mkdirs()
        directory.resolve("ServerBuildConfig.kt").writeText(
            """
            package com.eunilsung.ods.data.remote.server

            /** local.properties 의 server.baseUrl(또는 ODS_SERVER_BASE_URL)에서 빌드 시점에 생성됨. 직접 수정하지 말 것. */
            internal const val SERVER_BASE_URL: String = "${serverBaseUrl.get()}"
            """.trimIndent() + "\n"
        )
    }
}

val generateServerConfig by tasks.registering(GenerateServerConfigTask::class) {
    serverBaseUrl.set(serverBaseUrlProperty)
    outputDir.set(layout.buildDirectory.dir("generated/serverConfig/commonMain/kotlin"))
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.biometric)
        }
        commonMain {
            kotlin.srcDir(generateServerConfig)
        }
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.krypto)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.eunilsung.ods.db")
        }
    }
}

compose.resources {
    packageOfResClass = "ods.composeapp.generated.resources"
}

android {
    namespace = "com.eunilsung.ods"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eunilsung.ods"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

/** 도메인 모델은 Compose 를 모른다 — 안정적인 값이라는 사실을 설정 파일로 알려 화면 건너뛰기를 유지한다. */
composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose-stability.conf"))
}

/**
 * 기능 테스트 — 앱의 실제 통신 코드와 서버 모드 저장소를 진짜 서버에 붙여 돌린다(`commonTest/.../functest`).
 * `-PfunctionalTest=true` 를 줄 때만 서버 주소가 테스트에 전달된다. 주지 않으면 시나리오는 아무 요청 없이 지나간다.
 *
 * ```
 * ./gradlew :composeApp:testDebugUnitTest -PfunctionalTest=true --tests "*functest*"
 * ./gradlew :composeApp:iosSimulatorArm64Test -PfunctionalTest=true
 * ```
 *
 * 주소를 따로 주지 않으면 로컬 서버([localServerUrl])에 붙고, 떠 있지 않으면 빌드가 띄웠다가 끝날 때 내린다.
 * 다른 서버에 돌리려면 `-PfunctionalTestUrl=<주소>` 를 준다. 환경변수 `ODS_FUNCTEST_URL` 을 직접 줘도 같다.
 *
 * 진짜 서버의 상태를 보는 테스트라, 주소가 있을 때는 지난 결과를 다시 쓰지 않고 매번 돌린다.
 */
val functionalTestUrl: String = when {
    providers.gradleProperty("functionalTest").orNull == "true" ->
        providers.gradleProperty("functionalTestUrl").orNull ?: localServerUrl
    else -> providers.environmentVariable("ODS_FUNCTEST_URL").orNull.orEmpty()
}

tasks.withType<Test>().configureEach {
    inputs.property("functestUrl", functionalTestUrl)
    if (functionalTestUrl.isNotBlank()) {
        environment("ODS_FUNCTEST_URL", functionalTestUrl)
        outputs.upToDateWhen { false }
        if (functionalTestUrl == localServerUrl) dependsOn(":server:startLocalServer")
    }
}

/**
 * 실화면 흐름과 iOS 테스트에 쓸 시뮬레이터(UDID 또는 이름). 같은 PC 의 다른 작업이 쓰는 시뮬레이터를 피하려면
 * `local.properties` 의 `e2e.iosDevice` 에 적어 둔다. 없으면 iOS 테스트는 Kotlin 이 고른 기기에서 돈다.
 */
val iosTestDevice: String? = providers.environmentVariable("ODS_IOS_DEVICE").orNull
    ?: localProperties.getProperty("e2e.iosDevice")

/**
 * 시뮬레이터 테스트는 `simctl spawn` 으로 실행되므로 그냥 넣은 환경변수는 전달되지 않는다.
 * `SIMCTL_CHILD_` 접두어를 붙여야 자식 프로세스에 들어간다.
 */
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    if (functionalTestUrl.isNotBlank()) {
        environment("SIMCTL_CHILD_ODS_FUNCTEST_URL", functionalTestUrl)
        outputs.upToDateWhen { false }
        if (functionalTestUrl == localServerUrl) dependsOn(":server:startLocalServer")
    }
    iosTestDevice?.let { device.set(it) }
}

/**
 * 실화면 흐름(`.maestro`)을 기기에 돌린다. 기기를 고르고 켜고 앱을 설치하고 Maestro 를 부르는 일은 `.maestro/run.sh` 가 한다.
 * 앱은 로컬 서버 주소로 빌드되고([serverBaseUrlProperty]), 로컬 서버는 빌드가 도는 동안 떠 있다.
 * 기기 상태를 보는 작업이라 매번 돈다.
 */
val maestroRunner = rootProject.file(".maestro/run.sh").absolutePath

val e2eAndroid by tasks.registering(Exec::class) {
    group = "verification"
    description = "실화면 흐름을 Android 에뮬레이터에 돌린다"
    dependsOn("assembleDebug", ":server:startLocalServer")
    commandLine("bash", maestroRunner, "android", layout.buildDirectory.file("outputs/apk/debug/composeApp-debug.apk").get().asFile.absolutePath)
    environment("ODS_SERVER_BASE_URL", serverBaseUrlProperty)
    outputs.upToDateWhen { false }
}

val e2eIos by tasks.registering(Exec::class) {
    group = "verification"
    description = "실화면 흐름을 iOS 시뮬레이터에 돌린다"
    dependsOn(":server:startLocalServer")
    commandLine("bash", maestroRunner, "ios")
    environment("ODS_SERVER_BASE_URL", serverBaseUrlProperty)
    outputs.upToDateWhen { false }
}

/**
 * 전체 검증 — 서버 테스트 → 앱 테스트와 기능 테스트(JVM) → 앱 테스트와 기능 테스트(iOS 시뮬레이터) → 실화면 흐름(Android → iOS).
 *
 * ```
 * ./gradlew :composeApp:verifyAll -PfunctionalTest=true --continue
 * ```
 *
 * Android Studio 에서는 실행 구성 `검증 - 전체 (실화면 포함)` 을 고른다. `--continue` 로 돌리면 앞이 깨져도 끝까지 돌고,
 * 깨진 단계가 마지막에 모여 나온다. 기기는 `local.properties` 의 `e2e.androidAvd`·`e2e.iosDevice` 로 고르고, 꺼져 있으면 켠다.
 */
val verifyAll by tasks.registering {
    group = "verification"
    description = "서버·앱·기능 테스트(JVM·iOS)와 실화면 흐름(Android·iOS)을 한 번에 돌린다"
    dependsOn(":server:test", "testDebugUnitTest", "iosSimulatorArm64Test", e2eAndroid, e2eIos)
}
tasks.named { it == "testDebugUnitTest" }.configureEach { mustRunAfter(":server:test") }
tasks.named { it == "iosSimulatorArm64Test" }.configureEach { mustRunAfter("testDebugUnitTest") }
e2eAndroid { mustRunAfter("iosSimulatorArm64Test") }
e2eIos { mustRunAfter(e2eAndroid) }

/** 오래 도는 검증이 한참 뒤에 설정 탓으로 깨지지 않게, 요청을 받은 자리에서 먼저 확인한다. */
if (e2eRequested) {
    check(serverBaseUrlProperty == localServerUrl) {
        "실화면 흐름은 로컬 서버($localServerUrl)에 붙는 앱이 필요하다. ODS_SERVER_BASE_URL 을 빼거나 그 주소로 준다 (지금: '$serverBaseUrlProperty')"
    }
}
if ("verifyAll" in requestedTasks) {
    check(functionalTestUrl.isNotBlank()) { "verifyAll 은 기능 테스트를 켜고 돌린다: -PfunctionalTest=true" }
}
