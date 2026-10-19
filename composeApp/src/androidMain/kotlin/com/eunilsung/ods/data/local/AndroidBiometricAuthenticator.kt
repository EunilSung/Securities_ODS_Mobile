package com.eunilsung.ods.data.local

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android 생체 인증 — [BiometricPrompt].
 *
 * 주문 인증이라 BIOMETRIC_STRONG(Class 3)만 받는다. 얼굴 인식 중 보안 등급이 낮은 것은 쓰지 않고 비밀번호로 넘어간다.
 */
class AndroidBiometricAuthenticator(
    private val context: Context,
) : BiometricAuthenticator {

    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG

    override fun isAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun authenticate(
        title: String,
        subtitle: String,
        negativeText: String,
    ): BiometricResult = withContext(Dispatchers.Main) {
        val activity = AndroidActivityHolder.activity
        if (activity == null || !isAvailable()) {
            Log.message("[Biometric/Android] unavailable activity=${activity != null}")
            return@withContext BiometricResult.UNAVAILABLE
        }

        suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(BiometricResult.SUCCESS)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val canceled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        if (continuation.isActive) {
                            continuation.resume(if (canceled) BiometricResult.CANCELED else BiometricResult.UNAVAILABLE)
                        }
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeText)
                .setAllowedAuthenticators(authenticators)
                .build()
            runCatching { prompt.authenticate(info) }.onFailure {
                Log.message("[Biometric/Android] authenticate failed: ${it.message}")
                if (continuation.isActive) continuation.resume(BiometricResult.UNAVAILABLE)
            }
            continuation.invokeOnCancellation { runCatching { prompt.cancelAuthentication() } }
        }
    }
}
