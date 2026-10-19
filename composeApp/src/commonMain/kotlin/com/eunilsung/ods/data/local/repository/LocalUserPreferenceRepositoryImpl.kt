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

    private val _maskPersonalInfo = MutableStateFlow(settings.getBoolean(KEY_MASK_PERSONAL_INFO, true))
    override val maskPersonalInfo: StateFlow<Boolean> = _maskPersonalInfo.asStateFlow()

    override fun setMaskPersonalInfo(mask: Boolean) {
        settings.putBoolean(KEY_MASK_PERSONAL_INFO, mask)
        _maskPersonalInfo.value = mask
    }

    private companion object {
        const val KEY_LIST_DENSITY = "pref.listDensity"
        const val KEY_MASK_PERSONAL_INFO = "pref.maskPersonalInfo"
    }
}
