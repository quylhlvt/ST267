package com.oc.catemoji.catoc.ui.onboarding.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oc.catemoji.catoc.core.helper.SharedPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _readyToNavigate = MutableSharedFlow<Unit>(replay = 1)
    val readyToNavigate = _readyToNavigate.asSharedFlow()

    private var isTimerRunning = false

    fun startSplashTimer(
        isOnline: Boolean,
        waitForOnline: suspend () -> Unit,
        waitForImages: suspend () -> Unit
    ) {
        if (isTimerRunning) return
        isTimerRunning = true

        viewModelScope.launch {
            if (isOnline) {
                // Có mạng: đợi data + image xong, tối đa 8s để tránh treo
                withTimeoutOrNull(8_000L) {
                    waitForOnline()
                    waitForImages()
                }
            }

            // Data xong rồi mới đợi thêm 3s
            delay(3_000L)

            isTimerRunning = false
            _readyToNavigate.emit(Unit)
        }
    }
}