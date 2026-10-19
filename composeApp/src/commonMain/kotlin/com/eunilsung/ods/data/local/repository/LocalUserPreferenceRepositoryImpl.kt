package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.repository.UserPreferenceRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 사용자 선택을 기기 설정 저장소에 남긴다. */
class LocalUserPreferenceRepositoryImpl(
    private val settings: Settings,
) : UserPreferenceRepository {

    private val _listDensity = MutableStateFlow(
        settings.getStringOrNull(KEY_LIST_DENSITY)
            ?.let { saved -> ListDensity.entries.firstOrNull { it.name == saved } }
            ?: ListDensity.COMFORTABLE
    )
    override val listDensity: StateFlow<ListDensity> = _listDensity.asStateFlow()

    override fun setListDensity(density: ListDensity) {
        settings.putString(KEY_LIST_DENSITY, density.name)
        _listDensity.value = density
    }

    override fun lastQuoteCode(): String? = settings.getStringOrNull(KEY_LAST_QUOTE_CODE)

    override fun setLastQuoteCode(code: String) {
        settings.putString(KEY_LAST_QUOTE_CODE, code)
    }

    private val _hideAmounts = MutableStateFlow(settings.getBoolean(KEY_HIDE_AMOUNTS, false))
    override val hideAmounts: StateFlow<Boolean> = _hideAmounts.asStateFlow()

    override fun setHideAmounts(hide: Boolean) {
        settings.putBoolean(KEY_HIDE_AMOUNTS, hide)
        _hideAmounts.value = hide
    }

    private companion object {
        const val KEY_LIST_DENSITY = "pref.listDensity"
        const val KEY_LAST_QUOTE_CODE = "pref.lastQuoteCode"
        const val KEY_HIDE_AMOUNTS = "pref.hideAmounts"
    }
}
