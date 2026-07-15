package com.oc.catemoji.catoc.ui.main.frameDesign.addFrame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class FrameDesignViewModel @Inject constructor(
    private val appDataManager: AppDataManager
) : ViewModel() {

    private val _onlineFramePaths = MutableStateFlow<List<String>>(emptyList())
    val onlineFramePaths: StateFlow<List<String>> = _onlineFramePaths.asStateFlow()

    private var isLoading = false
    private var hasLoaded = false

    fun loadOnlineFramePaths() {
        if (isLoading || hasLoaded) return
        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(FRAME_CONFIG_URL).openConnection() as HttpURLConnection
                val json = try {
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }

                val backgrounds = JSONObject(json).getJSONArray("background")
                val paths = buildList {
                    for (categoryIndex in 0 until backgrounds.length()) {
                        val item = backgrounds.getJSONObject(categoryIndex)
                        val category = item.optString("category").trim('/')
                        val quantity = item.optInt("quantity").coerceIn(0, MAX_FRAME_COUNT)
                        val categoryPath = if (category.isEmpty()) "" else "$category/"

                        for (imageIndex in 1..quantity) {
                            add("$FRAME_BASE_URL/$categoryPath$imageIndex.png")
                        }
                    }
                }

                _onlineFramePaths.value = paths
                hasLoaded = true
            } catch (_: Exception) {
                _onlineFramePaths.value = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    companion object {
        private const val FRAME_BASE_URL =
            "https://lvtglobal.tech/public/app/ST267_CoupleCreatorsDressUp2/imageFrame"
        private const val FRAME_CONFIG_URL = "$FRAME_BASE_URL/imageFrame.json"
        private const val MAX_FRAME_COUNT = 100
    }
}
