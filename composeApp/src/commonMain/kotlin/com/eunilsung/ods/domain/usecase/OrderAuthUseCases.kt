package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.PinCheckResult
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.domain.repository.OrderAuthRepository
import kotlin.time.Clock

class ObserveOrderAuthConfigUseCase(private val repository: OrderAuthRepository) {
    operator fun invoke() = repository.config
}

/** 지금 잠겨 있으면 풀리는 시각, 아니면 null. 잠금 시각이 지났으면 잠기지 않은 것으로 본다. */
class GetOrderAuthLockUseCase(private val repository: OrderAuthRepository) {
    operator fun invoke(): Long? =
        repository.config.value.lockedUntilEpochSeconds?.takeIf { it > Clock.System.now().epochSeconds }
}

class RegisterOrderPinUseCase(private val repository: OrderAuthRepository) {
    operator fun invoke(pin: String) = repository.setPin(pin)
}

class VerifyOrderPinUseCase(private val repository: OrderAuthRepository) {
    operator fun invoke(pin: String): PinCheckResult = repository.verifyPin(pin, Clock.System.now().epochSeconds)
}

class SetOrderBiometricEnabledUseCase(private val repository: OrderAuthRepository) {
    operator fun invoke(enabled: Boolean) = repository.setBiometricEnabled(enabled)
}

class IsBiometricAvailableUseCase(private val authenticator: BiometricAuthenticator) {
    operator fun invoke(): Boolean = authenticator.isAvailable()
}

class AuthenticateBiometricUseCase(private val authenticator: BiometricAuthenticator) {
    suspend operator fun invoke(title: String, subtitle: String, negativeText: String): BiometricResult =
        authenticator.authenticate(title, subtitle, negativeText)
}

/** 주문 인증 화면이 쓰는 유스케이스 묶음. */
class OrderAuthUseCases(
    val observeConfig: ObserveOrderAuthConfigUseCase,
    val getLock: GetOrderAuthLockUseCase,
    val registerPin: RegisterOrderPinUseCase,
    val verifyPin: VerifyOrderPinUseCase,
    val setBiometricEnabled: SetOrderBiometricEnabledUseCase,
    val isBiometricAvailable: IsBiometricAvailableUseCase,
    val authenticateBiometric: AuthenticateBiometricUseCase,
)
