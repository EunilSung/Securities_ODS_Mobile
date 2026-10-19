package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.repository.LoginRepository
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

class LoginUseCase(private val repository: LoginRepository) {
    suspend operator fun invoke(request: LoginRequest): LoginResult =
        runCatching { repository.login(request, Clock.System.now().epochSeconds) }.getOrDefault(LoginResult.Unreachable)
}

class LogoutUseCase(private val repository: LoginRepository) {
    operator fun invoke() = repository.logout()
}

/** 로그인한 고객. 로그인 전이면 null. */
class ObserveCustomerUseCase(private val repository: LoginRepository) {
    operator fun invoke(): StateFlow<Customer?> = repository.currentCustomer
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
    val observeCustomer: ObserveCustomerUseCase,
    val getSavedId: GetSavedIdUseCase,
    val isSaveId: IsSaveIdUseCase,
    val setSaveId: SetSaveIdUseCase,
    val getAppInfo: GetAppInfoUseCase,
)
