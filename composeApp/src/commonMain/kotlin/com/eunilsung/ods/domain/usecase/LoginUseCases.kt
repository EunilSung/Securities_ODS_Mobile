package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.repository.AppLockRepository
import com.eunilsung.ods.domain.repository.LoginRepository
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

/** 로그인한다. 되면 그 직원의 앱 잠금 세션을 연다 — 비밀번호를 정하지 않았으면 등록 화면이 뜬다. */
class LoginUseCase(
    private val repository: LoginRepository,
    private val appLock: AppLockRepository,
) {
    suspend operator fun invoke(request: LoginRequest): LoginResult {
        val result = runCatching { repository.login(request, Clock.System.now().epochSeconds) }.getOrDefault(LoginResult.Unreachable)
        if (result is LoginResult.Success) appLock.startSession(result.staff.id, nowMillis())
        return result
    }
}

class LogoutUseCase(
    private val repository: LoginRepository,
    private val appLock: AppLockRepository,
) {
    operator fun invoke() {
        appLock.endSession()
        repository.logout()
    }
}

/** 로그인한 직원. 로그인 전이면 null. */
class ObserveStaffUseCase(private val repository: LoginRepository) {
    operator fun invoke(): StateFlow<Staff?> = repository.currentStaff
}

class GetSavedIdUseCase(private val repository: LoginRepository) {
    operator fun invoke(): String? = repository.savedId()
}

class IsSaveIdUseCase(private val repository: LoginRepository) {
    operator fun invoke(): Boolean = repository.isSaveId()
}

class SetSaveIdUseCase(private val repository: LoginRepository) {
    operator fun invoke(save: Boolean) = repository.setSaveId(save)
}

/** 로그인 화면이 쓰는 유스케이스 묶음. */
class LoginUseCases(
    val login: LoginUseCase,
    val observeStaff: ObserveStaffUseCase,
    val getSavedId: GetSavedIdUseCase,
    val isSaveId: IsSaveIdUseCase,
    val setSaveId: SetSaveIdUseCase,
    val getAppInfo: GetAppInfoUseCase,
    val getLogoutReason: GetLogoutReasonUseCase,
    val clearLogoutReason: ClearLogoutReasonUseCase,
)
