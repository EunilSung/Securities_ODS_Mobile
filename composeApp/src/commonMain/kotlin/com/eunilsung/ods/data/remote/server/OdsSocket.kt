package com.eunilsung.ods.data.remote.server

import com.eunilsung.ods.Config
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

/** 서버 알림 통로. 저장소들이 이 인터페이스로 알림을 듣는다 — 테스트에서는 대역을 끼운다. */
interface ServerEvents {
    /** 서버가 보낸 알림. */
    val events: SharedFlow<ServerEvent>

    /** 연결될 때마다 한 번씩 나온다. 끊긴 동안의 알림은 다시 오지 않으므로 받으면 목록을 다시 받는다. */
    val connected: SharedFlow<Unit>

    /** 연결을 유지하며 알림을 받는다. 부른 쪽이 취소할 때까지 끝나지 않는다. */
    suspend fun listen()
}

/**
 * WebSocket 알림 수신. 받기만 한다 — 바꾸는 일은 모두 REST 다. 끊기면 간격을 늘려 가며 다시 붙는다.
 */
class OdsSocket(
    private val httpClient: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val baseUrl: String = Config.Server.BASE_URL,
) : ServerEvents {

    private val json = Json { ignoreUnknownKeys = true }

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = EVENT_BUFFER)
    override val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    private val _connected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val connected: SharedFlow<Unit> = _connected.asSharedFlow()

    override suspend fun listen() {
        var backoffMs = MIN_BACKOFF_MS
        while (true) {
            val url = baseUrl.replaceFirst("http", "ws") + "/ws?token=" + tokenStore.token().encodeURLParameter()
            try {
                httpClient.webSocket(url) {
                    Log.message("[Socket] connected")
                    backoffMs = MIN_BACKOFF_MS
                    _connected.emit(Unit)
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        runCatching { json.decodeFromString(ServerEvent.serializer(), frame.readText()) }
                            .getOrNull()
                            ?.let { _events.emit(it) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.message("[Socket] failed — ${e.message}")
            }
            Log.message("[Socket] disconnected — retry in ${backoffMs}ms")
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    private companion object {
        const val MIN_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
        const val EVENT_BUFFER = 64
    }
}
