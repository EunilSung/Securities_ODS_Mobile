package com.eunilsung.ods.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.usecase.AccountOpeningUseCases
import com.eunilsung.ods.domain.usecase.IdCheckResult
import com.eunilsung.ods.domain.usecase.OpenAccountResult
import com.eunilsung.ods.domain.util.AccountOpeningPolicy
import com.eunilsung.ods.domain.util.rrnBackFirst
import com.eunilsung.ods.domain.util.rrnFront
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 계좌 개설 — 신분증 촬영 → 본인 확인 → 계좌 종류 → 약관 동의 → 계좌 비밀번호(고객 입력) → 서명 → 완료.
 *
 * - 본인 확인: 직원이 신분증을 보고 이름·주민번호 앞 6자리·뒷자리 첫 숫자를 옮겨 적는다. 고객 정보와 맞으면 진위를 확인한다.
 *   사진과 적은 내용은 확인에만 쓰고 남기지 않는다. 사진을 다시 찍으면 확인 결과도 버린다.
 * - 계좌 종류: 고객이 열 수 없는 종류(ISA 나이·중복 등)는 까닭과 함께 막는다.
 * - 계좌 비밀번호: 고객이 보안 키패드로 두 번 넣는다. 약하거나 생년월일이 들어간 비밀번호는 받지 않고, 기기에 남기지 않는다.
 */
class AccountOpeningViewModel(
    private val useCases: AccountOpeningUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountOpeningUiState())
    val uiState: StateFlow<AccountOpeningUiState> = _uiState.asStateFlow()

    private var watchJob: Job? = null
    private var firstPin: String? = null

    fun onAction(action: AccountOpeningActions) {
        when (action) {
            is AccountOpeningActions.OnEnter -> enter(action.customerId)
            is AccountOpeningActions.OnPhotoCaptured -> if (action.photo != null) {
                _uiState.update { it.copy(photo = action.photo, isDemoId = false, verification = null, idMismatches = emptyList(), idError = null) }
            }
            AccountOpeningActions.OnUseDemoId -> useDemoId()
            AccountOpeningActions.OnRetake -> _uiState.update {
                it.copy(photo = null, isDemoId = false, verification = null, idMismatches = emptyList(), idError = null, step = OpeningStep.ID_CAPTURE)
            }
            is AccountOpeningActions.OnIdTypeChange -> editId { it.copy(idType = action.type) }
            is AccountOpeningActions.OnIdNameChange -> editId { it.copy(idName = action.name.take(MAX_NAME)) }
            is AccountOpeningActions.OnRrnFrontChange -> editId { it.copy(rrnFront = action.digits.filter(Char::isDigit).take(6)) }
            is AccountOpeningActions.OnRrnBackChange -> editId { it.copy(rrnBackFirst = action.digit.filter(Char::isDigit).take(1)) }
            AccountOpeningActions.OnVerify -> verify()
            is AccountOpeningActions.OnTypeSelect -> _uiState.update { it.copy(accountType = action.type, error = null) }
            is AccountOpeningActions.OnTermToggle -> _uiState.update {
                it.copy(agreedTerms = if (action.terms in it.agreedTerms) it.agreedTerms - action.terms else it.agreedTerms + action.terms)
            }
            AccountOpeningActions.OnAgreeAll -> _uiState.update {
                it.copy(agreedTerms = if (it.agreedTerms.size == Terms.entries.size) emptySet() else Terms.entries.toSet())
            }
            is AccountOpeningActions.OnPinEntered -> pinEntered(action.pin)
            is AccountOpeningActions.OnSignatureChange -> _uiState.update { it.copy(signature = action.signature, error = null) }
            AccountOpeningActions.OnNext -> _uiState.update {
                if (it.canProceed && it.step < OpeningStep.SIGN) it.copy(step = OpeningStep.entries[it.step.ordinal + 1], error = null) else it
            }
            AccountOpeningActions.OnPrevious -> _uiState.update {
                if (it.step == OpeningStep.ID_CAPTURE || it.step == OpeningStep.DONE) it
                else it.copy(step = OpeningStep.entries[it.step.ordinal - 1], error = null)
            }
            is AccountOpeningActions.OnSubmit -> submit(action.note)
        }
    }

    private fun enter(customerId: String) {
        if (_uiState.value.customer?.id == customerId) return
        useCases.startConsult(customerId)
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            combine(useCases.observeCustomer(customerId), useCases.observeAccounts(customerId)) { customer, accounts -> customer to accounts }
                .collect { (customer, accounts) ->
                    val today = todayInSeoul()
                    _uiState.update { state ->
                        state.copy(
                            customer = customer,
                            typeBlocks = if (customer == null) emptyMap() else AccountType.entries.mapNotNull { type ->
                                AccountOpeningPolicy.blockFor(type, customer, accounts, today)?.let { type to it }
                            }.toMap(),
                        )
                    }
                }
        }
    }

    /** 신분증 칸을 고치면 이전 확인 결과는 버린다. */
    private fun editId(change: (AccountOpeningUiState) -> AccountOpeningUiState) =
        _uiState.update { change(it).copy(verification = null, idMismatches = emptyList(), idError = null) }

    private fun useDemoId() {
        val customer = _uiState.value.customer ?: return
        _uiState.update {
            it.copy(
                photo = null,
                isDemoId = true,
                idName = customer.name,
                rrnFront = customer.rrnFront(),
                rrnBackFirst = customer.rrnBackFirst().toString(),
                verification = null,
                idMismatches = emptyList(),
                idError = null,
            )
        }
    }

    private fun verify() {
        val state = _uiState.value
        val customer = state.customer ?: return
        if (!state.canVerify) return
        _uiState.update { it.copy(isVerifying = true, idMismatches = emptyList(), idError = null) }
        viewModelScope.launch {
            val info = IdCardInfo(state.idType, state.idName, state.rrnFront, state.rrnBackFirst.first())
            val result = useCases.verifyIdCard(customer.id, info, state.photo)
            _uiState.update {
                when (result) {
                    is IdCheckResult.Verified -> it.copy(isVerifying = false, verification = result.verification)
                    is IdCheckResult.Mismatch -> it.copy(isVerifying = false, idMismatches = result.mismatches)
                    IdCheckResult.Rejected -> it.copy(isVerifying = false, idError = IdError.REJECTED)
                    IdCheckResult.Unreachable -> it.copy(isVerifying = false, idError = IdError.UNREACHABLE)
                }
            }
        }
    }

    private fun pinEntered(pin: String) {
        val state = _uiState.value
        val customer = state.customer ?: return
        when (state.pinStage) {
            PinStage.ENTER ->
                if (AccountOpeningPolicy.isWeakPin(pin, customer.birthDate)) {
                    _uiState.update { it.copy(pinNotice = PinNotice.WEAK, pinResetKey = it.pinResetKey + 1) }
                } else {
                    firstPin = pin
                    _uiState.update { it.copy(pinStage = PinStage.CONFIRM, pinNotice = null) }
                }
            PinStage.CONFIRM ->
                if (pin == firstPin) {
                    firstPin = null
                    _uiState.update { it.copy(pinStage = PinStage.DONE, pinNotice = null) }
                } else {
                    firstPin = null
                    _uiState.update { it.copy(pinStage = PinStage.ENTER, pinNotice = PinNotice.MISMATCH, pinResetKey = it.pinResetKey + 1) }
                }
            PinStage.DONE -> Unit
        }
    }

    private fun submit(note: String) {
        val state = _uiState.value
        val customer = state.customer ?: return
        val type = state.accountType ?: return
        if (state.step != OpeningStep.SIGN || !state.canProceed) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = useCases.openAccount(
                customerId = customer.id,
                type = type,
                verification = state.verification,
                agreedTerms = state.agreedTerms,
                pinSet = state.pinStage == PinStage.DONE,
                signature = state.signature,
                note = note,
            )
            _uiState.update {
                when (result) {
                    is OpenAccountResult.Opened -> it.copy(isSaving = false, step = OpeningStep.DONE, opened = result.account, photo = null)
                    OpenAccountResult.IdNotVerified -> it.copy(isSaving = false, error = OpenError.ID_NOT_VERIFIED)
                    is OpenAccountResult.TypeBlocked -> it.copy(isSaving = false, error = OpenError.TYPE_BLOCKED)
                    OpenAccountResult.TermsRequired -> it.copy(isSaving = false, error = OpenError.TERMS)
                    OpenAccountResult.PinRequired -> it.copy(isSaving = false, error = OpenError.PIN)
                    OpenAccountResult.SignatureRequired -> it.copy(isSaving = false, error = OpenError.SIGNATURE)
                    OpenAccountResult.NotSignedIn -> it.copy(isSaving = false, error = OpenError.NOT_SIGNED_IN)
                    OpenAccountResult.NotFound -> it.copy(isSaving = false, error = OpenError.NOT_FOUND)
                }
            }
        }
    }

    private companion object {
        const val MAX_NAME = 30
    }
}
