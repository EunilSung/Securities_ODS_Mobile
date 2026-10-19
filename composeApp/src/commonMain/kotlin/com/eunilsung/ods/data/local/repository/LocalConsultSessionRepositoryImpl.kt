package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.domain.repository.ConsultSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 상담 중인 고객을 메모리에만 둔다. 앱을 다시 켜면 비어 있다. */
class LocalConsultSessionRepositoryImpl : ConsultSessionRepository {
    private val _activeCustomerId = MutableStateFlow<String?>(null)
    override val activeCustomerId: StateFlow<String?> = _activeCustomerId.asStateFlow()

    override fun start(customerId: String) {
        _activeCustomerId.value = customerId
    }

    override fun end() {
        _activeCustomerId.value = null
    }
}
