package com.eunilsung.ods.data.local

import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

/**
 * iOS 생체 인증 — LocalAuthentication(Face ID·Touch ID).
 *
 * 기기 암호로 대신 푸는 정책은 쓰지 않는다. 생체가 안 되면 앱의 주문 비밀번호로 넘어간다.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBiometricAuthenticator : BiometricAuthenticator {

    override fun isAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        negativeText: String,
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val context = LAContext()
        context.localizedCancelTitle = negativeText
        context.localizedFallbackTitle = ""
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
            if (continuation.isActive) continuation.resume(BiometricResult.UNAVAILABLE)
            return@suspendCancellableCoroutine
        }
        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = subtitle.ifBlank { title },
        ) { success, error ->
            val result = when {
                success -> BiometricResult.SUCCESS
                error != null -> when (error.code) {
                    LA_ERROR_USER_CANCEL, LA_ERROR_USER_FALLBACK, LA_ERROR_SYSTEM_CANCEL, LA_ERROR_APP_CANCEL -> BiometricResult.CANCELED
                    LA_ERROR_NOT_AVAILABLE, LA_ERROR_NOT_ENROLLED, LA_ERROR_LOCKOUT -> BiometricResult.UNAVAILABLE
                    else -> BiometricResult.FAILED
                }
                else -> BiometricResult.FAILED
            }
            if (continuation.isActive) continuation.resume(result)
        }
        continuation.invokeOnCancellation { context.invalidate() }
    }

    private companion object {
        /** LAError 코드(LAError.h). */
        const val LA_ERROR_USER_CANCEL = -2L
        const val LA_ERROR_USER_FALLBACK = -3L
        const val LA_ERROR_SYSTEM_CANCEL = -4L
        const val LA_ERROR_NOT_AVAILABLE = -6L
        const val LA_ERROR_NOT_ENROLLED = -7L
        const val LA_ERROR_LOCKOUT = -8L
        const val LA_ERROR_APP_CANCEL = -9L
    }
}
