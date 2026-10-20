import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.eunilsung.ods.server.ApplicationKt")
}

dependencies {
    implementation(projects.shared)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.logback.classic)

    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.embedded.postgres)
    testImplementation(platform(libs.embedded.postgres.binaries.bom))
    testRuntimeOnly(libs.embedded.postgres.binaries.darwin.arm64)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

/**
 * 로컬 서버 포트. 기본 8095 — 앱(`server.baseUrl`)과 기능 테스트·실화면 흐름의 기본 주소가 이 포트를 본다.
 * 같은 PC 의 다른 프로젝트 서버가 8095 을 쓰고 있으면 `-PlocalServerPort=<포트>` 로 피한다. 앱 빌드(composeApp)도 같은 속성을 본다.
 */
val localServerPort: String = providers.gradleProperty("localServerPort").orNull ?: "8095"

/**
 * 로컬 개발용 실행 — 내장 PostgreSQL 을 함께 띄운다.
 *
 * PostgreSQL 을 따로 설치하지 않아도 서버가 뜬다. 데이터는 `server/.localdb` 에 남아 다시 켜도 유지된다.
 * 내장 PostgreSQL 은 테스트 의존성이라 배포본(`installDist`)에는 들어가지 않는다.
 *
 *   ./gradlew :server:runLocal
 */
tasks.register<JavaExec>("runLocal") {
    group = "application"
    description = "내장 PostgreSQL 과 함께 서버를 띄운다 (로컬 개발용)"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.eunilsung.ods.server.local.LocalServerKt")
    workingDir = projectDir
    environment("PORT", localServerPort)
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
}

/**
 * 빌드가 도는 동안만 떠 있는 로컬 서버.
 *
 * 기능 테스트를 버튼 하나로 돌리려면 서버가 먼저 떠 있어야 한다. 이미 떠 있으면(직접 `runLocal` 을
 * 띄워 둔 경우) 그대로 쓰고 건드리지 않는다. 여기서 띄운 것만 빌드가 끝날 때 내린다.
 */
abstract class LocalServerService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    private var process: Process? = null
    private var dataDirectory: File? = null
    private var healthUrl = ""

    @Synchronized
    fun startIfDown(javaExecutable: String, classpath: String, workingDir: File, logFile: File, port: String) {
        healthUrl = "http://localhost:$port/health"
        if (isUp()) return
        dataDirectory = File(workingDir, LOCAL_DB_DIRECTORY)
        logFile.parentFile.mkdirs()
        /** Windows 는 명령줄 길이에 한도가 있어 클래스패스를 파일로 넘긴다. */
        val argFile = File(logFile.parentFile, "local-server.args")
        argFile.writeText("-cp\n\"${classpath.replace("\\", "\\\\")}\"\n")
        val builder = ProcessBuilder(
            javaExecutable, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
            "@${argFile.absolutePath}", MAIN_CLASS,
        ).directory(workingDir)
            .redirectErrorStream(true).redirectOutput(logFile)
        builder.environment()["PORT"] = port
        process = builder.start()

        val deadline = System.currentTimeMillis() + START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (isUp()) return
            if (process?.isAlive != true) break
            Thread.sleep(POLL_MS)
        }
        close()
        throw GradleException("로컬 서버가 뜨지 않았다. 로그: ${logFile.absolutePath}")
    }

    /** 서버가 띄운 내장 PostgreSQL 까지 함께 내린다. 서버만 죽이면 DB 프로세스가 남아 다음 실행을 막는다. */
    @Synchronized
    override fun close() {
        val running = process ?: return
        running.descendants().forEach { it.destroyForcibly() }
        running.destroyForcibly()
        stopDatabase()
        process = null
    }

    /**
     * 내장 PostgreSQL 은 서버의 자식으로 잡히지 않고 따로 떠 있다. 데이터 폴더의 `postmaster.pid` 첫 줄이
     * 그 프로세스 번호다. 번호가 다른 프로그램에 재사용됐을 수 있으므로 postgres 일 때만 내린다.
     */
    private fun stopDatabase() {
        val pid = dataDirectory?.let { File(it, "postmaster.pid") }?.takeIf { it.exists() }
            ?.useLines { it.firstOrNull() }?.trim()?.toLongOrNull() ?: return
        ProcessHandle.of(pid)
            .filter { it.info().command().orElse("").contains("postgres", ignoreCase = true) }
            .ifPresent { it.destroyForcibly() }
    }

    private fun isUp(): Boolean = runCatching {
        val connection = URI(healthUrl).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = PROBE_TIMEOUT_MS
        connection.readTimeout = PROBE_TIMEOUT_MS
        connection.responseCode == 200
    }.getOrDefault(false)

    private companion object {
        const val MAIN_CLASS = "com.eunilsung.ods.server.local.LocalServerKt"
        const val LOCAL_DB_DIRECTORY = ".localdb"
        const val START_TIMEOUT_MS = 180_000L
        const val POLL_MS = 1_000L
        const val PROBE_TIMEOUT_MS = 2_000
    }
}

abstract class StartLocalServer : DefaultTask() {
    @get:Classpath
    abstract val serverClasspath: ConfigurableFileCollection

    @get:Internal
    abstract val javaExecutable: Property<String>

    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @get:Internal
    abstract val logFile: RegularFileProperty

    @get:Internal
    abstract val server: Property<LocalServerService>

    @get:Input
    abstract val port: Property<String>

    @TaskAction
    fun start() {
        server.get().startIfDown(
            javaExecutable.get(),
            serverClasspath.asPath,
            workingDirectory.get().asFile,
            logFile.get().asFile,
            port.get(),
        )
    }
}

val localServer = gradle.sharedServices.registerIfAbsent("localServer", LocalServerService::class) {}

/**
 * 로컬 서버가 떠 있지 않으면 띄우고, 뜰 때까지 기다린다. 기능 테스트가 이 작업에 기댄다.
 * 띄운 서버는 빌드가 끝나면 내려간다 — 계속 띄워 두려면 `runLocal` 을 쓴다.
 */
tasks.register<StartLocalServer>("startLocalServer") {
    group = "application"
    description = "기능 테스트가 붙을 로컬 서버를 빌드가 도는 동안 띄운다"
    serverClasspath.from(sourceSets.test.get().runtimeClasspath)
    javaExecutable.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }.map { it.executablePath.asFile.absolutePath }
    )
    workingDirectory.set(layout.projectDirectory)
    logFile.set(layout.buildDirectory.file("local-server/server.log"))
    server.set(localServer)
    port.set(localServerPort)
    usesService(localServer)
    outputs.upToDateWhen { false }
}
