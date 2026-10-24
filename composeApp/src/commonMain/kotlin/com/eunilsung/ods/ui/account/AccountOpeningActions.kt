package com.eunilsung.ods.ui.account

import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.IdCardType
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.Terms

sealed interface AccountOpeningActions {
    /** 계좌 개설을 시작했다. 이 고객과 상담 중으로 둔다. */
    data class OnEnter(val customerId: String) : AccountOpeningActions

    /** 카메라에서 돌아왔다. 취소했으면 null. */
    data class OnPhotoCaptured(val photo: ByteArray?) : AccountOpeningActions

    /** 시연용 신분증을 쓴다. 고객 정보로 신분증 칸을 채운다. */
    data object OnUseDemoId : AccountOpeningActions

    /** 다시 찍기. 찍은 사진과 확인 결과를 버린다. */
    data object OnRetake : AccountOpeningActions

    data class OnIdTypeChange(val type: IdCardType) : AccountOpeningActions
    data class OnIdNameChange(val name: String) : AccountOpeningActions

    /** 주민등록번호 앞 6자리. 숫자만 받는다. */
    data class OnRrnFrontChange(val digits: String) : AccountOpeningActions

    /** 주민등록번호 뒷자리 첫 숫자. */
    data class OnRrnBackChange(val digit: String) : AccountOpeningActions

    /** 신분증 확인(고객 정보와 맞추고 진위 확인). */
    data object OnVerify : AccountOpeningActions

    data class OnTypeSelect(val type: AccountType) : AccountOpeningActions
    data class OnTermToggle(val terms: Terms) : AccountOpeningActions

    /** 전체 동의(선택 약관 포함). 모두 동의돼 있으면 모두 푼다. */
    data object OnAgreeAll : AccountOpeningActions

    /** 고객이 보안 키패드로 계좌 비밀번호를 다 넣었다. */
    data class OnPinEntered(val pin: String) : AccountOpeningActions

    data class OnSignatureChange(val signature: Signature) : AccountOpeningActions
    data object OnNext : AccountOpeningActions
    data object OnPrevious : AccountOpeningActions

    /**
     * 계좌를 연다.
     *
     * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만든다.
     */
    data class OnSubmit(val note: String) : AccountOpeningActions
}
