package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import androidx.lifecycle.ViewModel
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddOneFrameViewModel  @Inject constructor(
    private val appDataManager: AppDataManager
) : ViewModel() {}