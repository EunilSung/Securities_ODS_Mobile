package com.eunilsung.ods.functest

import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.testsupport.createTestDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

/** 시뮬레이터 테스트는 `simctl spawn` 으로 돌아 Gradle 이 `SIMCTL_CHILD_ODS_FUNCTEST_URL` 로 넘긴 값이 여기서 보인다. */
@OptIn(ExperimentalForeignApi::class)
actual fun functionalTestServerUrl(): String? = getenv("ODS_FUNCTEST_URL")?.toKString()?.takeIf { it.isNotBlank() }

actual fun functionalTestHttpClient(): HttpClient = HttpClient(Darwin) { install(WebSockets) }

/** native 드라이버는 메모리 DB 도 동시에 쓸 수 있다. 다른 테스트와 같은 것을 쓴다. */
actual fun createFunctionalTestDatabase(): AppDatabase = createTestDatabase()
