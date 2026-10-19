package com.eunilsung.ods.domain.model

/** 생체 인증 결과. */
enum class BiometricResult {
    SUCCESS,

    /** 인식은 됐지만 맞지 않았다. */
    FAILED,

    /** 사용자가 취소했다(취소 버튼·시스템 취소). */
    CANCELED,

    /** 쓸 수 없다 — 기기 미지원, 등록된 생체 없음. */
    UNAVAILABLE,
}
