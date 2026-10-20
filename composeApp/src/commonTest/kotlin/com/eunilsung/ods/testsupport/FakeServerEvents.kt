package com.eunilsung.ods.testsupport

import com.eunilsung.ods.data.remote.server.ServerEvents
import com.eunilsung.ods.shared.api.ServerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 알림 통로 대역. 테스트가 [push] 와 [connect] 로 서버 알림과 (재)연결을 일으킨다.
 *
 * 저장소는 자기 스코프에서 구독을 시작하므로, 구독자가 [subscribers] 만큼 붙을 때까지 기다렸다가 보낸다.
 */
class FakeServerEvents(private val subscribers: Int = 1) : ServerEvents {

    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<ServerEvent> = _events

    private val _connected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val connected: SharedFlow<Unit> = _connected

    /** 지금 듣고 있는 수. 로그인하면 1, 로그아웃하면 0 이 된다. */
    var listening = 0
        private set

    override suspend fun listen() {
        listening++
        try {
            awaitCancellation()
        } finally {
            listening--
        }
    }

    suspend fun push(event: ServerEvent) {
        awaitSubscribers(_events)
        _events.emit(event)
    }

    suspend fun connect() {
        awaitSubscribers(_connected)
        _connected.emit(Unit)
    }

    private suspend fun awaitSubscribers(flow: MutableSharedFlow<*>) = withContext(Dispatchers.Default) {
        withTimeout(AWAIT_TIMEOUT_MS) { flow.subscriptionCount.first { it >= subscribers } }
    }
}

/** 저장소가 자기 스코프에서 돌리는 일이 끝나기를 실제 시간으로 기다린다. 시간 안에 안 되면 실패한다. */
suspend fun awaitUntil(condition: suspend () -> Boolean) = withContext(Dispatchers.Default) {
    withTimeout(AWAIT_TIMEOUT_MS) {
        while (!condition()) delay(AWAIT_POLL_MS)
    }
}

private const val AWAIT_TIMEOUT_MS = 5_000L
private const val AWAIT_POLL_MS = 10L
