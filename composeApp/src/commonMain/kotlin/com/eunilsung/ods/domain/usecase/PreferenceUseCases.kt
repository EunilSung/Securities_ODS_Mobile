package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.repository.UserPreferenceRepository

class ObserveListDensityUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke() = repository.listDensity
}

class SetListDensityUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke(density: ListDensity) = repository.setListDensity(density)
}

class ObserveMaskPersonalInfoUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke() = repository.maskPersonalInfo
}

class SetMaskPersonalInfoUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke(mask: Boolean) = repository.setMaskPersonalInfo(mask)
}
