package com.eunilsung.ods.ui.account

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.IdCardType
import com.eunilsung.ods.domain.model.IdMismatch
import com.eunilsung.ods.domain.model.IdVerification
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.util.AccountOpeningPolicy
import com.eunilsung.ods.domain.util.SignaturePolicy

/** 계좌 개설의 단계. */
enum class OpeningStep { ID_CAPTURE, ID_CHECK, ACCOUNT_TYPE, TERMS, PIN, SIGN, DONE }

/** 계좌 비밀번호 입력 단계. */
enum class PinStage { ENTER, CONFIRM, DONE }

/** 계좌 비밀번호 입력 알림. */
enum class PinNotice { WEAK, MISMATCH }

/**
 * 계좌 개설 화면의 상태.
 *
 * [photo] 는 배열이라 같은 사진인지 내용으로 비교하지 않는다(새로 찍으면 다른 사진으로 본다).
 */
data class AccountOpeningUiState(
    val step: OpeningStep = OpeningStep.ID_CAPTURE,
    val customer: Customer? = null,
    /** 찍은 신분증 사진. 메모리에만 두고, 개설을 마치거나 화면을 닫으면 버린다. */
    val photo: ByteArray? = null,
    /** 시연용 신분증을 쓰는지. */
    val isDemoId: Boolean = false,
    /** 서버 없이 도는 시연이라 시연용 신분증을 고를 수 있는지. */
    val offersDemoId: Boolean = false,
    val idType: IdCardType = IdCardType.RESIDENT,
    val idName: String = "",
    val rrnFront: String = "",
    val rrnBackFirst: String = "",
    val isVerifying: Boolean = false,
    val verification: IdVerification.Verified? = null,
    val idMismatches: List<IdMismatch> = emptyList(),
    val idError: IdError? = null,
    val accountType: AccountType? = null,
    /** 이 고객이 열 수 없는 종류와 그 까닭. */
    val typeBlocks: Map<AccountType, AccountTypeBlock> = emptyMap(),
    val agreedTerms: Set<Terms> = emptySet(),
    val pinStage: PinStage = PinStage.ENTER,
    val pinNotice: PinNotice? = null,
    /** 틀릴 때마다 바뀌어 키패드를 비우고 다시 섞는다. */
    val pinResetKey: Int = 0,
    val signature: Signature = Signature(emptyList()),
    val isSaving: Boolean = false,
    val error: OpenError? = null,
    val opened: Account? = null,
) {
    val hasPhoto: Boolean get() = photo != null || isDemoId

    val canVerify: Boolean get() = hasPhoto && idName.isNotBlank() && rrnFront.length == 6 && rrnBackFirst.length == 1 && !isVerifying

    val canProceed: Boolean
        get() = when (step) {
            OpeningStep.ID_CAPTURE -> hasPhoto
            OpeningStep.ID_CHECK -> verification != null
            OpeningStep.ACCOUNT_TYPE -> accountType != null && accountType !in typeBlocks
            OpeningStep.TERMS -> AccountOpeningPolicy.requiredTermsAgreed(agreedTerms)
            OpeningStep.PIN -> pinStage == PinStage.DONE
            OpeningStep.SIGN -> SignaturePolicy.isEnough(signature) && !isSaving
            OpeningStep.DONE -> false
        }

}

enum class IdError { REJECTED, UNREACHABLE }

enum class OpenError { ID_NOT_VERIFIED, TYPE_BLOCKED, TERMS, PIN, SIGNATURE, NOT_SIGNED_IN, NOT_FOUND }
