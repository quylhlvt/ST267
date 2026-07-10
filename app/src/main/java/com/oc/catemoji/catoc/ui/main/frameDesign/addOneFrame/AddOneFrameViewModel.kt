package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddOneFrameViewModel  @Inject constructor(
    private val appDataManager: AppDataManager
) : ViewModel() {

    private val _avatarList = MutableStateFlow<List<String>>(emptyList())
    val avatarList: StateFlow<List<String>> = _avatarList.asStateFlow()
    private val selectedImageDrafts = mutableMapOf<Int, SelectedImageDraft>()

    fun loadAvatarList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = runCatching {
                appDataManager.myDesignPaths.value
            }.onFailure {
                Log.e("AddOneFrameViewModel", "loadAvatarList failed", it)
            }.getOrDefault(emptyList())

            withContext(Dispatchers.Main) {
                _avatarList.value = list
            }
        }
    }

    suspend fun saveFrameDesign(imagePath: String) {
        appDataManager.addMyFrameDesignPath(imagePath)
    }

    fun saveSelectedImage(
        index: Int,
        path: String,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float
    ) {
        selectedImageDrafts[index] = SelectedImageDraft(
            path = path,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation
        )
    }

    fun getSelectedImageDrafts(): Map<Int, SelectedImageDraft> = selectedImageDrafts.toMap()

    data class SelectedImageDraft(
        val path: String,
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
        val rotation: Float
    )
}
