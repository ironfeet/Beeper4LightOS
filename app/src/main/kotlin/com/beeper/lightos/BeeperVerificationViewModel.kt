package com.beeper.lightos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveDeviceVerification
import net.folivo.trixnity.client.verification.ActiveSasVerificationState
import net.folivo.trixnity.client.verification.ActiveVerificationState
import com.thelightphone.sdk.LightViewModel

class BeeperVerificationViewModel : LightViewModel<Unit>() {
    private val client: MatrixClient
        get() = BeeperRepository.getClient()!!

    private val _isVerified = MutableStateFlow(true) // assume true until we check
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _activeVerification = MutableStateFlow<ActiveDeviceVerification?>(null)
    val activeVerification: StateFlow<ActiveDeviceVerification?> = _activeVerification.asStateFlow()

    private val _verificationState = MutableStateFlow<ActiveVerificationState?>(null)
    val verificationState: StateFlow<ActiveVerificationState?> = _verificationState.asStateFlow()

    private val _sasVerificationState = MutableStateFlow<ActiveSasVerificationState?>(null)
    val sasVerificationState: StateFlow<ActiveSasVerificationState?> = _sasVerificationState.asStateFlow()

    init {
        viewModelScope.launch {
            client.key.getTrustLevel(client.userId, client.deviceId).collect { level ->
                _isVerified.value = level is net.folivo.trixnity.crypto.key.DeviceTrustLevel.CrossSigned && level.verified
            }
        }
        viewModelScope.launch {
            client.verification.activeDeviceVerification.collectLatest { verification ->
                _activeVerification.value = verification
                if (verification != null) {
                    verification.state.collectLatest { state ->
                        _verificationState.value = state
                        if (state is ActiveVerificationState.Start) {
                            val method = state.method
                            if (method is net.folivo.trixnity.client.verification.ActiveSasVerificationMethod) {
                                method.state.collectLatest { sasState ->
                                    _sasVerificationState.value = sasState
                                }
                            }
                        } else {
                            _sasVerificationState.value = null
                        }
                    }
                } else {
                    _verificationState.value = null
                    _sasVerificationState.value = null
                }
            }
        }
    }

    fun submitSecurityCode(code: String) {
        viewModelScope.launch {
            try {
                val methods = client.verification.getSelfVerificationMethods().first()
                if (methods is net.folivo.trixnity.client.verification.VerificationService.SelfVerificationMethods.CrossSigningEnabled) {
                    val recoveryKeyMethod = methods.methods.filterIsInstance<net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKey>().firstOrNull()
                    val passphraseMethod = methods.methods.filterIsInstance<net.folivo.trixnity.client.verification.SelfVerificationMethod.AesHmacSha2RecoveryKeyWithPbkdf2Passphrase>().firstOrNull()
                    
                    if (recoveryKeyMethod != null && code.replace("-", "").length >= 48) {
                        android.util.Log.d("BeeperVerification", "Using AesHmacSha2RecoveryKey verification method")
                        val result = recoveryKeyMethod.verify(code)
                        if (result.isFailure) {
                            android.util.Log.e("BeeperVerification", "Recovery Key verify failed", result.exceptionOrNull())
                        } else {
                            android.util.Log.d("BeeperVerification", "Recovery Key verify succeeded!")
                            
                            // Check secrets
                            android.util.Log.d("BeeperVerification", "Recovery Key verify succeeded! (Skipped logging secrets due to koin get issue)")
                        }
                    } else if (passphraseMethod != null) {
                        android.util.Log.d("BeeperVerification", "Using AesHmacSha2RecoveryKeyWithPbkdf2Passphrase verification method")
                        val result = passphraseMethod.verify(code)
                        if (result.isFailure) {
                            android.util.Log.e("BeeperVerification", "Passphrase verify failed", result.exceptionOrNull())
                        } else {
                            android.util.Log.d("BeeperVerification", "Passphrase verify succeeded!")
                        }
                    } else {
                        android.util.Log.e("BeeperVerification", "No suitable recovery key method found")
                    }
                } else {
                    android.util.Log.e("BeeperVerification", "Cross signing not enabled or not ready: $methods")
                }
            } catch (e: Exception) {
                android.util.Log.e("BeeperVerification", "Failed to verify with recovery key", e)
            }
        }
    }

    fun requestVerification() {
        viewModelScope.launch {
            try {
                android.util.Log.d("BeeperVerification", "Calling createDeviceVerificationRequest...")
                val devices = client.api.device.getDevices().getOrNull() ?: emptyList()
                val otherDeviceIds = devices.map { it.deviceId }.filter { it != client.deviceId }.toSet()
                android.util.Log.d("BeeperVerification", "Other devices to target: $otherDeviceIds")
                val result = client.verification.createDeviceVerificationRequest(client.userId, otherDeviceIds)
                if (result.isFailure) {
                    android.util.Log.e("BeeperVerification", "Request failed", result.exceptionOrNull())
                } else {
                    android.util.Log.d("BeeperVerification", "Request succeeded!")
                }
            } catch (e: Exception) {
                android.util.Log.e("BeeperVerification", "Request threw exception", e)
            }
        }
    }

    fun acceptRequest(state: ActiveVerificationState.TheirRequest) {
        viewModelScope.launch {
            state.ready()
        }
    }
    
    fun startSas(state: ActiveVerificationState.Ready) {
        viewModelScope.launch {
            state.start(net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod.Sas)
        }
    }
    
    fun acceptSas(state: ActiveSasVerificationState.TheirSasStart) {
        viewModelScope.launch {
            state.accept()
        }
    }
    
    fun match(state: ActiveSasVerificationState.ComparisonByUser) {
        viewModelScope.launch {
            state.match()
        }
    }
    
    fun noMatch(state: ActiveSasVerificationState.ComparisonByUser) {
        viewModelScope.launch {
            state.noMatch()
        }
    }

    fun cancelVerification() {
        viewModelScope.launch {
            _activeVerification.value?.cancel()
        }
    }

    fun resetVerification() {
        _activeVerification.value = null
        _verificationState.value = null
        _sasVerificationState.value = null
    }
}
