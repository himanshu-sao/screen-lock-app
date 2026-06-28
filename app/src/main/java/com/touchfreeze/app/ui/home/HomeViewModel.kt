package com.touchfreeze.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchfreeze.app.data.EncryptedPinRepository
import com.touchfreeze.app.domain.FreezeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isPinSet: Boolean = false,
    val authErrorMessage: String? = null,
    val unlockProgress: Float = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pinRepository: EncryptedPinRepository
) : ViewModel() {

    private val _freezeState = MutableStateFlow<FreezeState>(FreezeState.Idle)
    val freezeState: StateFlow<FreezeState> = _freezeState.asStateFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPinStatus()
    }

    private fun loadPinStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPinSet = pinRepository.isPinSet()
            )
        }
    }

    fun onPlayVideo() {
        _freezeState.value = FreezeState.Playing
    }

    fun onFreeze() {
        _freezeState.value = FreezeState.Frozen
    }

    fun onUnlockGestureDetected() {
        _freezeState.value = FreezeState.UnlockGestureDetected

        viewModelScope.launch {
            delay(300)
            _freezeState.value = FreezeState.AuthChallenge
        }
    }

    fun onPinEntered(rawPin: String) {
        viewModelScope.launch {
            if (!pinRepository.isPinSet()) {
                // No PIN set, just unlock without checking
                _freezeState.value = FreezeState.Unlocked
                _uiState.value = _uiState.value.copy(authErrorMessage = null)
                return@launch
            }

            val isValid = pinRepository.verifyPin(rawPin)
            if (isValid) {
                _freezeState.value = FreezeState.Unlocked
                _uiState.value = _uiState.value.copy(authErrorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(
                    authErrorMessage = "Incorrect PIN"
                )
            }
        }
    }

    fun clearAuthError() {
        _uiState.value = _uiState.value.copy(authErrorMessage = null)
    }

    fun scheduleRefreeze() {
        viewModelScope.launch {
            delay(5000)
            if (_freezeState.value == FreezeState.Unlocked) {
                _freezeState.value = FreezeState.Frozen
            }
        }
    }

    fun onExitToHome() {
        _freezeState.value = FreezeState.Idle
    }

    fun onReset() {
        _freezeState.value = FreezeState.Frozen
        _uiState.value = _uiState.value.copy(authErrorMessage = null)
    }

    fun updateUnlockProgress(progress: Float) {
        _uiState.value = _uiState.value.copy(unlockProgress = progress.coerceIn(0f, 1f))
    }
}
