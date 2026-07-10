package com.oc.catemoji.catoc.data.model.frameDesign

import android.graphics.Bitmap

data class CropImage(
    val bitmap: Bitmap,
    val path: String = "",
    var scale: Float = 1f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var rotation: Float = 0f
)
