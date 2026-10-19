package com.eunilsung.ods.ui.customer.register

import com.eunilsung.ods.domain.model.Gender

sealed interface NewCustomerActions {
    data class OnNameChange(val name: String) : NewCustomerActions

    /** 생년월일 8자리(19850314). 숫자만 받는다. */
    data class OnBirthChange(val digits: String) : NewCustomerActions
    data class OnGenderChange(val gender: Gender) : NewCustomerActions

    /** 휴대폰 번호. 숫자만 받는다. */
    data class OnPhoneChange(val digits: String) : NewCustomerActions
    data class OnAddressChange(val address: String) : NewCustomerActions
    data object OnSubmit : NewCustomerActions
}

/** 신규 고객 등록이 한 번씩 처리할 일. */
sealed interface NewCustomerEvent {
    /** 등록했다. 그 고객의 상세를 연다. */
    data class Registered(val customerId: String) : NewCustomerEvent
}
