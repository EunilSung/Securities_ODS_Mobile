package com.eunilsung.ods.ui.customer.register

import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.util.formatPhone
import com.eunilsung.ods.domain.util.parseBirthDate

/** 신규 고객 등록 양식의 상태. */
data class NewCustomerUiState(
    val name: String = "",
    val birthDigits: String = "",
    val gender: Gender = Gender.MALE,
    val phoneDigits: String = "",
    val address: String = "",
    /** 등록을 한 번 눌렀는지. 그 전에는 칸이 비어 있어도 오류를 보이지 않는다. */
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    /** 서버에 닿지 못했거나 서버가 받아 주지 않아 등록하지 못했다. 적은 내용은 그대로 두어 다시 누를 수 있다. */
    val saveFailed: Boolean = false,
) {
    val nameError: Boolean get() = name.isBlank()
    val birthError: Boolean get() = parseBirthDate(birthDigits) == null
    val phoneError: Boolean get() = formatPhone(phoneDigits) == null
    val hasError: Boolean get() = nameError || birthError || phoneError
}
