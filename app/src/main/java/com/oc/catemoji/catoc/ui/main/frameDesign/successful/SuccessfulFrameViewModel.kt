package com.oc.catemoji.catoc.ui.main.frameDesign.successful

import androidx.lifecycle.ViewModel
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SuccessfulFrameViewModel  @Inject constructor(
    private val appDataManager: AppDataManager
) : ViewModel() {}