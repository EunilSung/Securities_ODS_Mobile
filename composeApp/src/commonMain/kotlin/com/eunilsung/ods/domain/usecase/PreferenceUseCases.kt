package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.repository.UserPreferenceRepository

class ObserveListDensityUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke() = repository.listDensity
}

class SetListDensityUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke(density: ListDensity) = repository.setListDensity(density)
}

class ObserveHideAmountsUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke() = repository.hideAmounts
}

class SetHideAmountsUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke(hide: Boolean) = repository.setHideAmounts(hide)
}
