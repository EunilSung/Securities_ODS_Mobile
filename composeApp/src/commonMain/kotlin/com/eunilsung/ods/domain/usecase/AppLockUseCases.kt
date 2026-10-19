package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.AppLockState
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.domain.model.UnlockResult
import com.eunilsung.ods.domain.repository.AppLockRepository
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.domain.repository.LocalDataWiper
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.util.AppLockPolicy
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

class ObserveAppLockUseCase(private val repository: AppLockRepository) {
    operator fun invoke(): StateFlow<AppLockState> = repository.state
}

class RegisterAppPinUseCase(private val repository: AppLockRepository) {
    operator fun invoke(pin: String) = repository.setPin(pin)
}

/**
 * 잠금 비밀번호로 푼다. [AppLockPolicy.MAX_ATTEMPTS] 번 틀리면 단말 분실로 보고
 * 기기 안 업무 데이터와 모든 잠금 비밀번호를 지운 뒤 로그아웃한다.
 */
class UnlockWithPinUseCase(
    private val repository: AppLockRepository,
    private val wiper: LocalDataWiper,
    private val logout: LogoutUseCase,
) {
    suspend operator fun invoke(pin: String): UnlockResult =
        when (val check = repository.checkPin(pin)) {
            AppLockRepository.PinCheck.Matched -> {
                repository.unlock()
                UnlockResult.Unlocked
            }
            is AppLockRepository.PinCheck.Wrong ->
                if (check.failures >= AppLockPolicy.MAX_ATTEMPTS) {
                    wiper.wipe()
                    repository.clearAll()
                    logout(LogoutReason.WIPED)
                    UnlockResult.Wiped
                } else {
                    UnlockResult.Wrong(AppLockPolicy.MAX_ATTEMPTS - check.failures)
                }
        }
}

/** 생체 인증으로 푼다. 성공하면 잠금을 푼다. */
class UnlockWithBiometricUseCase(
    private val repository: AppLockRepository,
    private val authenticator: BiometricAuthenticator,
) {
    suspend operator fun invoke(title: String, subtitle: String, negativeText: String): BiometricResult {
        val result = authenticator.authenticate(title, subtitle, negativeText)
        if (result == BiometricResult.SUCCESS) repository.unlock()
        return result
    }
}

class SetAppBiometricEnabledUseCase(private val repository: AppLockRepository) {
    operator fun invoke(enabled: Boolean) = repository.setBiometricEnabled(enabled)
}

class IsBiometricAvailableUseCase(private val authenticator: BiometricAuthenticator) {
    operator fun invoke(): Boolean = authenticator.isAvailable()
}

class AuthenticateBiometricUseCase(private val authenticator: BiometricAuthenticator) {
    suspend operator fun invoke(title: String, subtitle: String, negativeText: String): BiometricResult =
        authenticator.authenticate(title, subtitle, negativeText)
}

class LockAppUseCase(private val repository: AppLockRepository) {
    operator fun invoke() = repository.lock()
}

class RecordActivityUseCase(private val repository: AppLockRepository) {
    operator fun invoke(now: Long = nowMillis()) = repository.recordActivity(now)
}

class RecordBackgroundUseCase(private val repository: AppLockRepository) {
    operator fun invoke(now: Long = nowMillis()) = repository.recordBackground(now)
}

/**
 * 오래 조작이 없었거나 백그라운드에 오래 있다 돌아왔으면 잠근다. 비밀번호를 정하지 않았으면 잠그지 않는다
 * (그때는 비밀번호 등록 화면이 이미 덮고 있다).
 *
 * @return 이번에 잠갔는지.
 */
class LockIfInactiveUseCase(private val repository: AppLockRepository) {
    operator fun invoke(now: Long = nowMillis()): Boolean {
        val state = repository.state.value
        if (!state.isPinSet || state.isLocked) {
            repository.takeBackgroundSince()
            return false
        }
        val backgroundSince = repository.takeBackgroundSince()
        val awayTooLong = backgroundSince != null && now - backgroundSince >= AppLockPolicy.BACKGROUND_LOCK_SECONDS * 1000
        val idleTooLong = now - repository.lastActivityMillis >= AppLockPolicy.IDLE_LOCK_SECONDS * 1000
        if (!awayTooLong && !idleTooLong) return false
        repository.lock()
        return true
    }
}

class GetLogoutReasonUseCase(private val repository: LoginRepository) {
    operator fun invoke() = repository.lastLogoutReason.value
}

class ClearLogoutReasonUseCase(private val repository: LoginRepository) {
    operator fun invoke() = repository.clearLogoutReason()
}

/** 잠금 화면이 쓰는 유스케이스 묶음. */
class AppLockUseCases(
    val observeAppLock: ObserveAppLockUseCase,
    val observeStaff: ObserveStaffUseCase,
    val registerPin: RegisterAppPinUseCase,
    val unlockWithPin: UnlockWithPinUseCase,
    val unlockWithBiometric: UnlockWithBiometricUseCase,
    val setBiometricEnabled: SetAppBiometricEnabledUseCase,
    val isBiometricAvailable: IsBiometricAvailableUseCase,
    val logout: LogoutUseCase,
)

/** 메인 화면이 쓰는 유스케이스 묶음 — 로그인·잠금 상태, 자동 잠금, 첫 시세. */
class MainUseCases(
    val observeStaff: ObserveStaffUseCase,
    val observeAppLock: ObserveAppLockUseCase,
    val observeListDensity: ObserveListDensityUseCase,
    val recordActivity: RecordActivityUseCase,
    val recordBackground: RecordBackgroundUseCase,
    val lockIfInactive: LockIfInactiveUseCase,
    val fetchWatchlist: FetchWatchlistUseCase,
    val refreshCustomers: RefreshCustomersUseCase,
    val refreshVisits: RefreshVisitsUseCase,
)
