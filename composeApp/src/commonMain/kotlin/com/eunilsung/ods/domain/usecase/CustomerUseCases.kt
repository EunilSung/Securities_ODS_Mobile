package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.repository.ConsultSessionRepository
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.repository.LoginRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 저장소에 쓰는 [block] 을 부른다. 저장소가 던지면(서버에 닿지 못했거나 서버가 받아 주지 않았다) null.
 *
 * 부른 쪽의 취소([CancellationException])는 삼키지 않는다.
 */
internal suspend fun <T> orNullOnFailure(block: suspend () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

class ObserveCustomersUseCase(private val repository: CustomerRepository) {
    operator fun invoke(): Flow<List<Customer>> = repository.observeCustomers()
}

class RefreshCustomersUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(): Boolean = runCatching { repository.refresh() }.getOrDefault(false)
}

class ObserveCustomersRefreshingUseCase(private val repository: CustomerRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.isRefreshing
}

class ObserveCustomerUseCase(private val repository: CustomerRepository) {
    operator fun invoke(id: String): Flow<Customer?> = repository.observeCustomer(id)
}

class ObserveConsultationsUseCase(private val repository: CustomerRepository) {
    operator fun invoke(customerId: String): Flow<List<Consultation>> = repository.observeConsultations(customerId)
}

class ObserveAccountsUseCase(private val repository: CustomerRepository) {
    operator fun invoke(customerId: String): Flow<List<Account>> = repository.observeAccounts(customerId)
}

/**
 * 새 고객을 등록하고 바로 그 고객과 상담을 시작한다 — 현장에서 처음 만난 고객을 등록하는 경우다.
 *
 * 서버에 닿지 못했거나 서버가 받아 주지 않으면 등록하지 않고 null.
 */
class RegisterCustomerUseCase(
    private val repository: CustomerRepository,
    private val session: ConsultSessionRepository,
) {
    suspend operator fun invoke(customer: NewCustomer, now: Long = nowMillis()): Customer? =
        orNullOnFailure { repository.register(customer, now) }?.also { session.start(it.id) }
}

/** 상담 기록 결과. */
sealed interface AddConsultationResult {
    data class Added(val consultation: Consultation) : AddConsultationResult

    /** 로그인이 풀려 기록할 직원이 없다. */
    data object NotSignedIn : AddConsultationResult

    /** 서버에 닿지 못했거나 서버가 받아 주지 않았다. */
    data object Unreachable : AddConsultationResult
}

/** 상담 기록을 남긴다. 기록한 직원은 로그인한 직원이다. */
class AddConsultationUseCase(
    private val repository: CustomerRepository,
    private val login: LoginRepository,
) {
    suspend operator fun invoke(consultation: NewConsultation, now: Long = nowMillis()): AddConsultationResult {
        val staff = login.currentStaff.value ?: return AddConsultationResult.NotSignedIn
        val added = orNullOnFailure { repository.addConsultation(consultation, staff.name, now) } ?: return AddConsultationResult.Unreachable
        return AddConsultationResult.Added(added)
    }
}

class ObserveActiveCustomerIdUseCase(private val session: ConsultSessionRepository) {
    operator fun invoke(): StateFlow<String?> = session.activeCustomerId
}

/** 이 고객과 상담을 시작한다(고객 상세를 열었다). */
class StartConsultUseCase(private val session: ConsultSessionRepository) {
    operator fun invoke(customerId: String) = session.start(customerId)
}

class EndConsultUseCase(private val session: ConsultSessionRepository) {
    operator fun invoke() = session.end()
}

/** 고객 목록 화면이 쓰는 유스케이스 묶음. */
class CustomerListUseCases(
    val observeCustomers: ObserveCustomersUseCase,
    val refresh: RefreshCustomersUseCase,
    val observeRefreshing: ObserveCustomersRefreshingUseCase,
    val observeMaskPersonalInfo: ObserveMaskPersonalInfoUseCase,
    val observeActiveCustomerId: ObserveActiveCustomerIdUseCase,
)

/** 고객 상세 화면이 쓰는 유스케이스 묶음. */
class CustomerDetailUseCases(
    val observeCustomer: ObserveCustomerUseCase,
    val observeConsultations: ObserveConsultationsUseCase,
    val observeAccounts: ObserveAccountsUseCase,
    val observeMaskPersonalInfo: ObserveMaskPersonalInfoUseCase,
    val setMaskPersonalInfo: SetMaskPersonalInfoUseCase,
    val startConsult: StartConsultUseCase,
    val observeUpcomingVisits: ObserveUpcomingVisitsUseCase,
    val observeSubscriptions: ObserveSubscriptionsUseCase,
)

/** 상담 칸(넓은 태블릿 오른쪽)과 상담 기록 화면이 쓰는 유스케이스 묶음. */
class ConsultUseCases(
    val observeActiveCustomerId: ObserveActiveCustomerIdUseCase,
    val observeCustomer: ObserveCustomerUseCase,
    val observeConsultations: ObserveConsultationsUseCase,
    val addConsultation: AddConsultationUseCase,
    val endConsult: EndConsultUseCase,
)
