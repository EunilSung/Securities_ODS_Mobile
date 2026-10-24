package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdMismatch
import com.eunilsung.ods.domain.model.IdVerification
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.repository.IdVerificationRepository
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.domain.util.AccountOpeningPolicy
import com.eunilsung.ods.domain.util.SignaturePolicy
import com.eunilsung.ods.domain.util.mismatchesWith
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.flow.first

/** 신분증 확인 결과. */
sealed interface IdCheckResult {
    data class Verified(val verification: IdVerification.Verified) : IdCheckResult

    /** 고객 정보와 어긋난다. 발급기관에 묻지 않았다. */
    data class Mismatch(val mismatches: List<IdMismatch>) : IdCheckResult

    data object Rejected : IdCheckResult
    data object Unreachable : IdCheckResult
}

/**
 * 신분증을 확인한다 — 먼저 고객 정보(이름·생년월일·성별)와 맞춰 보고, 맞으면 발급기관에 진위를 묻는다.
 *
 * 사진은 확인에만 넘기고 어디에도 남기지 않는다.
 */
class VerifyIdCardUseCase(
    private val customers: CustomerRepository,
    private val idVerification: IdVerificationRepository,
) {
    suspend operator fun invoke(customerId: String, info: IdCardInfo, photo: ByteArray?, now: Long = nowMillis()): IdCheckResult {
        val customer = customers.observeCustomer(customerId).first() ?: return IdCheckResult.Mismatch(listOf(IdMismatch.NAME))
        val mismatches = info.mismatchesWith(customer)
        if (mismatches.isNotEmpty()) return IdCheckResult.Mismatch(mismatches)
        return when (val result = runCatching { idVerification.verify(info, photo, now) }.getOrDefault(IdVerification.Unreachable)) {
            is IdVerification.Verified -> IdCheckResult.Verified(result)
            IdVerification.Rejected -> IdCheckResult.Rejected
            IdVerification.Unreachable -> IdCheckResult.Unreachable
        }
    }
}

/** 계좌 개설 결과. */
sealed interface OpenAccountResult {
    data class Opened(val account: Account) : OpenAccountResult
    data object IdNotVerified : OpenAccountResult
    data class TypeBlocked(val block: AccountTypeBlock) : OpenAccountResult
    data object TermsRequired : OpenAccountResult

    /** 고객이 계좌 비밀번호를 정하지 않았다. */
    data object PinRequired : OpenAccountResult
    data object SignatureRequired : OpenAccountResult
    data object NotSignedIn : OpenAccountResult
    data object NotFound : OpenAccountResult
}

/**
 * 계좌를 연다 — 신분증 확인·계좌 종류 제한·필수 약관·비밀번호·서명을 다시 확인하고, 계좌·서명 기록·상담 이력을 남긴다.
 *
 * 계좌 비밀번호는 고객이 직접 넣고, 기기에는 남기지 않는다.
 *
 * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만들어 넘긴다.
 */
class OpenAccountUseCase(
    private val customers: CustomerRepository,
    private val signatures: SignatureRepository,
    private val login: LoginRepository,
) {
    suspend operator fun invoke(
        customerId: String,
        type: AccountType,
        verification: IdVerification.Verified?,
        agreedTerms: Set<Terms>,
        pinSet: Boolean,
        signature: Signature,
        note: String,
        now: Long = nowMillis(),
    ): OpenAccountResult {
        val staff = login.currentStaff.value ?: return OpenAccountResult.NotSignedIn
        val customer = customers.observeCustomer(customerId).first() ?: return OpenAccountResult.NotFound
        if (verification == null) return OpenAccountResult.IdNotVerified
        val accounts = customers.observeAccounts(customerId).first()
        AccountOpeningPolicy.blockFor(type, customer, accounts, todayInSeoul(now))?.let { return OpenAccountResult.TypeBlocked(it) }
        if (!AccountOpeningPolicy.requiredTermsAgreed(agreedTerms)) return OpenAccountResult.TermsRequired
        if (!pinSet) return OpenAccountResult.PinRequired
        if (!SignaturePolicy.isEnough(signature)) return OpenAccountResult.SignatureRequired

        val account = customers.openAccount(customerId, type, now)
        signatures.record(customerId, SignaturePurpose.ACCOUNT_OPENING, signature, now)
        customers.addConsultation(NewConsultation(customerId, ConsultChannel.VISIT, ConsultTopic.ACCOUNT_OPENING, note), staff.name, now)
        return OpenAccountResult.Opened(account)
    }
}

/** 계좌 개설 화면이 쓰는 유스케이스 묶음. */
class AccountOpeningUseCases(
    val observeCustomer: ObserveCustomerUseCase,
    val observeAccounts: ObserveAccountsUseCase,
    val verifyIdCard: VerifyIdCardUseCase,
    val openAccount: OpenAccountUseCase,
    val startConsult: StartConsultUseCase,
)
