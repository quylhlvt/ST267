package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import com.oc.catemoji.catoc.data.model.mypony.MyAlbumModel
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

    private val _avatarList = MutableStateFlow<List<MyAlbumModel>>(emptyList())
    val avatarList: StateFlow<List<MyAlbumModel>> = _avatarList.asStateFlow()

    fun loadAvatarList() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = runCatching {
                appDataManager.customizedCharacters.value
                    .filter { it.imageSave.isNotEmpty() && File(it.imageSave).exists() }
                    .sortedByDescending { it.updatedAt }
                    .map {
                        MyAlbumModel(
                            path = it.imageSave,
                            idEdit = it.id,
                            type = 1
                        )
                    }
            }.onFailure {
                Log.e("AddOneFrameViewModel", "loadAvatarList failed", it)
            }.getOrDefault(emptyList())

            withContext(Dispatchers.Main) {
                _avatarList.value = list
            }
        }
    }
}
