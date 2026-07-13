package com.oc.catemoji.catoc.ui.onboarding.permission

import androidx.lifecycle.ViewModel
import com.oc.catemoji.catoc.core.helper.PermissionHelper
import com.oc.catemoji.catoc.core.helper.SharedPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    // Count is persisted so fragment-scoped and activity-scoped ViewModels stay in sync.
    private val _storageDenyCount = MutableStateFlow(
        SharedPreferencesManager.isPermissionStorRequest()
    )
    private val _notificationDenyCount = MutableStateFlow(
        SharedPreferencesManager.isPermissionNotiRequest()
    )

    val storageDenyCount: StateFlow<Int> = _storageDenyCount.asStateFlow()
    val notificationDenyCount: StateFlow<Int> = _notificationDenyCount.asStateFlow()

    fun onStorageDenied() {
        val count = SharedPreferencesManager.isPermissionStorRequest() + 1
        SharedPreferencesManager.setPermissionStorRequest(count)
        _storageDenyCount.value = count
    }

    fun onStorageGranted() {
        SharedPreferencesManager.setPermissionStorRequest(0)
        _storageDenyCount.value = 0
    }

    fun onNotificationDenied() {
        val count = SharedPreferencesManager.isPermissionNotiRequest() + 1
        SharedPreferencesManager.setPermissionNotiRequest(count)
        _notificationDenyCount.value = count
    }

    fun onNotificationGranted() {
        SharedPreferencesManager.setPermissionNotiRequest(0)
        _notificationDenyCount.value = 0
    }

    fun shouldGoToSettings(isStorage: Boolean): Boolean {
        val count = if (isStorage) {
            SharedPreferencesManager.isPermissionStorRequest()
        } else {
            SharedPreferencesManager.isPermissionNotiRequest()
        }
        return count >= 2
    }

    fun getStoragePermissions()      = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}
