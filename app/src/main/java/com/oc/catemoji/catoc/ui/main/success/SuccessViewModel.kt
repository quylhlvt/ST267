package com.oc.catemoji.catoc.ui.main.success

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oc.catemoji.catoc.core.helper.DownloadHelper
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject



// ViewViewModel.kt
@HiltViewModel
class SuccessViewModel @Inject constructor(private val appDataManager: AppDataManager) : ViewModel() {

    fun downloadFile(context: Context, path: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = DownloadHelper.downloadToGallery(context, path)
            onResult(success)
        }
    }

    fun deleteFile(
        path: String,
        isAvatar: Boolean,
        idEdit: String = "",
        imageType: Int = if (isAvatar) 1 else 2,
        onDone: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (imageType) {
                    1 -> if (idEdit.isNotEmpty()) {
                        appDataManager.deleteCustomizedCharacter(idEdit)
                    }
                    2 -> {
                        val current = appDataManager.myDesignPaths.value.toMutableList()
                        current.remove(path)
                        appDataManager.saveMyDesignToJson(current)
                    }
                    3 -> {
                        val current = appDataManager.myFrameDesignPaths.value.toMutableList()
                        current.remove(path)
                        appDataManager.saveMyFrameDesignToJson(current)
                    }
                }
                // Xóa file vật lý
                File(path).delete()
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
