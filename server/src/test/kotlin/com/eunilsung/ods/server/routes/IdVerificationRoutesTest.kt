package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.testsupport.authPost
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.loginToken
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.IdVerificationRequest
import com.eunilsung.ods.shared.api.IdVerificationResponse
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 신분증 진위 확인 흉내 — 이름에 "분실" 이 있으면 확인되지 않는다. */
class IdVerificationRoutesTest {

    @Test
    fun `보통 신분증은 확인되고 분실 신고된 신분증은 확인되지 않는다`() = serverTest {
        val token = client.loginToken("test1")

        val ok = client.verify(token, IdVerificationRequest("RESIDENT", "강하늘", "680314", "1"))
        val lost = client.verify(token, IdVerificationRequest("DRIVER", "분실테스트", "680314", "1"))

        assertEquals(HttpStatusCode.OK, ok.status)
        assertTrue(ok.decode(IdVerificationResponse.serializer()).verified)
        assertEquals(HttpStatusCode.OK, lost.status)
        assertFalse(lost.decode(IdVerificationResponse.serializer()).verified)
    }

    @Test
    fun `주민등록번호 형식이 틀리면 400 이다`() = serverTest {
        val token = client.loginToken("test1")

        for (request in listOf(
            IdVerificationRequest("RESIDENT", "강하늘", "68031", "1"),
            IdVerificationRequest("RESIDENT", "강하늘", "68031a", "1"),
            IdVerificationRequest("RESIDENT", "강하늘", "680314", "12"),
            IdVerificationRequest("RESIDENT", "강하늘", "680314", "x"),
        )) {
            assertEquals(HttpStatusCode.BadRequest, client.verify(token, request).status, request.toString())
        }
    }

    private suspend fun HttpClient.verify(token: String, request: IdVerificationRequest) =
        authPost(token, "/id-verifications", IdVerificationRequest.serializer(), request)
}
