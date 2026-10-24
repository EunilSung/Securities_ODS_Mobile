package com.eunilsung.ods.domain.model

/** 신분증 종류. */
enum class IdCardType { RESIDENT, DRIVER }

/**
 * 직원이 신분증을 보고 옮겨 적은 내용. 주민등록번호는 앞 6자리와 뒷자리 첫 숫자만 받는다 — 나머지는 받지도 남기지도 않는다.
 *
 * @param rrnFront 생년월일 6자리(YYMMDD).
 * @param rrnBackFirst 뒷자리 첫 숫자(성별·세기 구분).
 */
data class IdCardInfo(
    val type: IdCardType,
    val name: String,
    val rrnFront: String,
    val rrnBackFirst: Char,
)

/** 신분증과 고객 정보가 어긋난 곳. */
enum class IdMismatch { FORMAT, NAME, BIRTH, GENDER }

/** 신분증 진위 확인 결과. */
sealed interface IdVerification {
    /** 진위 확인을 마쳤다. 확인한 시각만 남긴다. */
    data class Verified(val type: IdCardType, val verifiedAtMillis: Long) : IdVerification

    /** 발급기관 기록과 맞지 않는다(분실 신고·재발급 등). */
    data object Rejected : IdVerification

    /** 확인 기관에 닿지 못했다. 다시 시도할 수 있다. */
    data object Unreachable : IdVerification
}

/** 약관. [required] 면 동의해야 계좌를 연다. */
enum class Terms(val required: Boolean) {
    ELECTRONIC_FINANCE(true),
    PERSONAL_INFO(true),
    ACCOUNT(true),
    MARKETING(false),
}

/** 계좌를 열 수 없는 까닭. */
enum class AccountTypeBlock {
    /** ISA 는 만 19세 이상(근로소득자는 15세)만 연다. */
    ISA_AGE,

    /** ISA 는 한 사람에 하나다. */
    ISA_DUPLICATE,

    /** 연금저축은 시연에서는 한 사람에 하나로 둔다. */
    PENSION_DUPLICATE,
}
