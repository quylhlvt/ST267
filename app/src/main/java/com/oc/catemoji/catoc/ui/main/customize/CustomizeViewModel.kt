package com.oc.catemoji.catoc.ui.main.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.oc.catemoji.catoc.data.datalocal.manager.AppDataManager
import com.oc.catemoji.catoc.data.model.custom.BodyPartModel
import com.oc.catemoji.catoc.data.model.custom.ColorModel
import com.oc.catemoji.catoc.data.model.custom.CustomModel
import com.oc.catemoji.catoc.data.model.custom.LayerTransform
import com.oc.catemoji.catoc.data.model.custom.SelectionIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI STATE ──────────────────────────────────────────────────────────────────

data class CustomizeState(
    val template:       CustomModel?        = null,
    val listData:       List<BodyPartModel> = emptyList(),

    /**
     * ✅ KEY: CHỈ LƯU INDEX – không lưu path string.
     * Path được resolve on-demand từ listData + selections.
     */
    val selections:     List<SelectionIndex> = emptyList(),
    val currentNavIndex: Int     = 0,
    val currentNavIndexChar1: Int = 0,
    val currentNavIndexChar2: Int = 0,
    val activeCharacter: Int     = 1,
    val isFlipped:       Boolean = false,
    val isLoading:       Boolean = true,
    val isSaving:        Boolean = false,
    val savedImagePath:  String? = null,
    val randomCount:     Int     = 0,
    val error:           String? = null
) {
    val currentColors: List<ColorModel>
        get() = listData.getOrNull(currentNavIndex)?.listPath ?: emptyList()

    val currentPaths: List<String>
        get() {
            val sel = selections.getOrNull(currentNavIndex) ?: return emptyList()
            return listData.getOrNull(currentNavIndex)
                ?.listPath?.getOrNull(sel.colorIndex)?.listPath ?: emptyList()
        }

    val currentColorIndex: Int get() = selections.getOrNull(currentNavIndex)?.colorIndex ?: 0
    val currentPathIndex:  Int get() = selections.getOrNull(currentNavIndex)?.pathIndex ?: 0
    val hasMultipleColors: Boolean get() = (listData.getOrNull(currentNavIndex)?.listPath?.size ?: 0) > 1
}

// ── VIEWMODEL ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val appDataManager: AppDataManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var editingCustomizedId: String? = null

    private val _state = MutableStateFlow(CustomizeState())
    val state: StateFlow<CustomizeState> = _state.asStateFlow()
    private var isInitialized = false
    private val _layerTransforms = MutableStateFlow<Map<Int, LayerTransform>>(emptyMap())
    val layerTransforms: StateFlow<Map<Int, LayerTransform>> = _layerTransforms.asStateFlow()

    fun getTransform(navIndex: Int) = _layerTransforms.value[navIndex] ?: LayerTransform()
    fun updateTransform(navIndex: Int, transform: LayerTransform) {
        _layerTransforms.update { it + (navIndex to transform) }
    }
    fun resetTransform(navIndex: Int) { _layerTransforms.update { it - navIndex } }
    fun isTransformDefault(navIndex: Int) = getTransform(navIndex) == LayerTransform()

    // ── INIT ──────────────────────────────────────────────────────────────────

    /** Khởi tạo với template mới (không có saved selections). */
    fun initNew(templateIndex: Int) {
        // ✅ Dùng state check thay vì flag — safe với process death
        if (_state.value.listData.isNotEmpty()) return
        editingCustomizedId = null
        val template = appDataManager.getCharacterByIndex(templateIndex) ?: return
        val sorted   = sortBodyParts(template.listPath)
        val draft = restoreDraft(template.id, sorted)
        val navChar1 = draft?.currentNavIndexChar1 ?: firstNavIndexForChar(sorted, 1)
        val navChar2 = draft?.currentNavIndexChar2 ?: firstNavIndexForChar(sorted, 2)
        val activeChar = draft?.activeCharacter?.takeIf { it == 1 || it == 2 } ?: 1
        _state.value = CustomizeState(
            template        = template,
            listData        = sorted,
            selections      = draft?.selections ?: buildDefaultSelections(sorted),
            currentNavIndex = if (activeChar == 1) navChar1 else navChar2,
            currentNavIndexChar1 = navChar1,
            currentNavIndexChar2 = navChar2,
            activeCharacter = activeChar,
            isFlipped       = draft?.isFlipped ?: false,
            isLoading       = true
        )
    }

    /** Khởi tạo để EDIT character đã lưu. */
    fun initEdit(templateIndex: Int, savedSelections: List<SelectionIndex>, isFlipped: Boolean) {
        if (_state.value.listData.isNotEmpty()) return
        val template = appDataManager.getCharacterByIndex(templateIndex) ?: return
        val sorted   = sortBodyParts(template.listPath)
        val navChar1 = firstNavIndexForChar(sorted, 1)
        val navChar2 = firstNavIndexForChar(sorted, 2)
        _state.value = CustomizeState(
            template        = template,
            listData        = sorted,
            selections      = clampSelections(sorted, savedSelections),
            isFlipped       = isFlipped,
            currentNavIndex = navChar1,
            currentNavIndexChar1 = navChar1,
            currentNavIndexChar2 = navChar2,
            activeCharacter = 1,
            isLoading       = true
        )
    }
    // CustomizeViewModel.kt — thêm hàm initWithSelections
// Xóa hàm initWithSelections sai đi, thay bằng:
    fun initWithSelections(templateIndex: Int, savedSelections: ArrayList<SelectionIndex>) {
        if (_state.value.listData.isNotEmpty()) return
        val template = appDataManager.getCharacterByIndex(templateIndex) ?: return
        val sorted   = sortBodyParts(template.listPath)
        val remapped = sorted.mapIndexed { sortedIdx, bp ->
            val originalIdx = template.listPath.indexOf(bp)
            val sel = savedSelections.getOrElse(originalIdx) { SelectionIndex(originalIdx, 0, 0) }
            SelectionIndex(sortedIdx, sel.colorIndex, sel.pathIndex)
        }
        val navChar1 = firstNavIndexForChar(sorted, 1)
        val navChar2 = firstNavIndexForChar(sorted, 2)
        _state.value = CustomizeState(
            template        = template,
            listData        = sorted,
            selections      = clampSelections(sorted, remapped),
            isFlipped       = false,
            currentNavIndex = navChar1,
            currentNavIndexChar1 = navChar1,
            currentNavIndexChar2 = navChar2,
            activeCharacter = 1,
            isLoading       = true
        )
    }
    fun initEditWithCustomizedId(
        templateIndex: Int,
        customizedId: String,
        savedSelections: List<SelectionIndex>,
        isFlipped: Boolean
    ) {
        if (_state.value.listData.isNotEmpty()) return
        editingCustomizedId = customizedId
        val template = appDataManager.getCharacterByIndex(templateIndex) ?: return
        _layerTransforms.value = appDataManager.getCharacterById(customizedId)?.layerTransforms.orEmpty()
        val sorted   = sortBodyParts(template.listPath)
        val navChar1 = firstNavIndexForChar(sorted, 1)
        val navChar2 = firstNavIndexForChar(sorted, 2)
        _state.value = CustomizeState(
            template        = template,
            listData        = sorted,
            selections      = clampSelections(sorted, savedSelections),
            isFlipped       = isFlipped,
            currentNavIndex = navChar1,
            currentNavIndexChar1 = navChar1,
            currentNavIndexChar2 = navChar2,
            activeCharacter = 1,
            isLoading       = true
        )
    }
    fun onLoadingComplete() = _state.update { it.copy(isLoading = false) }

    // ── SELECTIONS ────────────────────────────────────────────────────────────

    fun selectNav(navIndex: Int) {
        _state.update { state ->
            val safeNav = clampNavIndex(state.listData, state.activeCharacter, navIndex)
            when (state.activeCharacter) {
                2 -> state.copy(currentNavIndex = safeNav, currentNavIndexChar2 = safeNav)
                else -> state.copy(currentNavIndex = safeNav, currentNavIndexChar1 = safeNav)
            }
        }
        saveDraft()
    }
    fun toggleCharacter() {
        _state.update { state ->
            val nextChar = if (state.activeCharacter == 1) 2 else 1
            val storedNav = if (nextChar == 1) state.currentNavIndexChar1 else state.currentNavIndexChar2
            val safeNav = clampNavIndex(state.listData, nextChar, storedNav)
            state.copy(
                activeCharacter = nextChar,
                currentNavIndex = safeNav
            )
        }
        saveDraft()
    }
    fun toggleFlip() {
        _state.update { it.copy(isFlipped = !it.isFlipped) }
        saveDraft()
    }

    fun selectColor(colorIndex: Int) {
        updateSelection { state, old ->
            val bp        = state.listData.getOrNull(state.currentNavIndex) ?: return@updateSelection old
            val safeColor = colorIndex.coerceIn(0, bp.listPath.size - 1)
            val maxPath   = (bp.listPath.getOrNull(safeColor)?.listPath?.size ?: 1) - 1
            SelectionIndex(old.bodyPartIndex, safeColor, old.pathIndex.coerceIn(0, maxPath))
        }
    }

    fun selectPath(pathIndex: Int) {
        updateSelection { state, old ->
            val bp      = state.listData.getOrNull(state.currentNavIndex) ?: return@updateSelection old
            val maxPath = (bp.listPath.getOrNull(old.colorIndex)?.listPath?.size ?: 1) - 1
            SelectionIndex(old.bodyPartIndex, old.colorIndex, pathIndex.coerceIn(0, maxPath))
        }
    }

    fun selectNone() = selectPath(0)

    fun selectDiceCurrent() {
        updateSelection { state, old ->
            val bp    = state.listData.getOrNull(state.currentNavIndex) ?: return@updateSelection old
            val paths = bp.listPath.getOrNull(old.colorIndex)?.listPath ?: return@updateSelection old
            val start = startIndexAfterSpecial(paths)
            val idx   = if (paths.size > start) (start until paths.size).random() else start
            SelectionIndex(old.bodyPartIndex, old.colorIndex, idx)
        }
    }

    fun randomizeAll() {
        val state = _state.value
        val newSelections = state.listData.mapIndexed { i, bp ->
            val old = state.selections.getOrElse(i) { SelectionIndex(i, 0, 0) }
            if (bp.charType != state.activeCharacter) return@mapIndexed old

            val colorIdx = if (bp.listPath.size > 1) (0 until bp.listPath.size).random() else 0
            val paths    = bp.listPath.getOrNull(colorIdx)?.listPath ?: emptyList()
            val start    = startIndexAfterSpecial(paths)
            val pathIdx  = if (paths.size > start) (start until paths.size).random() else start
            SelectionIndex(i, colorIdx, pathIdx)
        }
        _state.update { it.copy(selections = newSelections, randomCount = it.randomCount + 1) }
        saveDraft()
    }

    fun resetAll() {
        val state = _state.value
        val navChar1 = firstNavIndexForChar(state.listData, 1)
        val navChar2 = firstNavIndexForChar(state.listData, 2)
        val activeChar = state.activeCharacter.takeIf { it == 1 || it == 2 } ?: 1
        _state.update {
            it.copy(
                selections = buildDefaultSelections(it.listData),
                isFlipped = false,
                currentNavIndexChar1 = navChar1,
                currentNavIndexChar2 = navChar2,
                currentNavIndex = if (activeChar == 1) navChar1 else navChar2
            )
        }
        _layerTransforms.value = emptyMap()
        saveDraft()
    }

    // ── PATH RESOLUTION ───────────────────────────────────────────────────────

    /** Lấy path thực tế tại bodyPartIndex. null = "none" hoặc "dice". */
    fun resolvePathAt(bodyPartIndex: Int): String? {
        val s    = _state.value
        val bp   = s.listData.getOrNull(bodyPartIndex) ?: return null
        val sel  = s.selections.getOrNull(bodyPartIndex) ?: return null
        val path = bp.listPath.getOrNull(sel.colorIndex)?.listPath?.getOrNull(sel.pathIndex) ?: return null
        return if (path == "none" || path == "dice") null else path
    }

    /** Resolve tất cả paths theo zIndex để render bitmap. */
    fun resolveAllCurrentPaths(): List<Pair<Int, String>> {
        val s = _state.value
        return s.listData.mapIndexedNotNull { i, bp ->
            val sel  = s.selections.getOrNull(i) ?: return@mapIndexedNotNull null
            val path = bp.listPath.getOrNull(sel.colorIndex)?.listPath?.getOrNull(sel.pathIndex)
                ?: return@mapIndexedNotNull null
            if (path == "none" || path == "dice") null else bp.zIndex to path
        }.sortedBy { it.first }
    }

    /** Lấy selections hiện tại để persist. ✅ Chỉ là danh sách index. */
    fun getCurrentSelections(): ArrayList<SelectionIndex> = ArrayList(_state.value.selections)

    // ── SAVE ──────────────────────────────────────────────────────────────────

    fun onSaveComplete(renderedImagePath: String): Pair<CustomModel, List<SelectionIndex>>? {
        val state    = _state.value
        val template = state.template ?: return null
        _state.update { it.copy(savedImagePath = renderedImagePath, isSaving = false) }

        val characterToSave = editingCustomizedId
            ?.let { id -> appDataManager.getCharacterById(id) }
            ?.copy(
                selections = ArrayList(state.selections),
                imageSave  = renderedImagePath,
                isFlipped  = state.isFlipped,
                layerTransforms = _layerTransforms.value,
                updatedAt  = System.currentTimeMillis()
            )
            ?: template.copy(layerTransforms = _layerTransforms.value)

        return characterToSave to state.selections
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private fun sortBodyParts(parts: List<BodyPartModel>) = parts.sortedBy { it.zIndex }

    private fun buildDefaultSelections(parts: List<BodyPartModel>): List<SelectionIndex> =
        parts.mapIndexed { i, bp ->
            val firstIndexForChar = parts.indexOfFirst { it.charType == bp.charType }
            if (i == firstIndexForChar) SelectionIndex(i, 0, 1) else SelectionIndex(i, 0, 0)
        }

    private fun clampSelections(parts: List<BodyPartModel>, saved: List<SelectionIndex>): List<SelectionIndex> =
        parts.mapIndexed { i, bp ->
            val s         = saved.getOrElse(i) { SelectionIndex(i, 0, 0) }
            val safeColor = s.colorIndex.coerceIn(0, maxOf(0, bp.listPath.size - 1))
            val maxPath   = maxOf(0, (bp.listPath.getOrNull(safeColor)?.listPath?.size ?: 1) - 1)
            SelectionIndex(i, safeColor, s.pathIndex.coerceIn(0, maxPath))
        }

    private fun startIndexAfterSpecial(paths: List<String>): Int = when {
        paths.firstOrNull() == "none" -> 2
        paths.firstOrNull() == "dice" -> 1
        else -> 0
    }

    private fun updateSelection(transform: (CustomizeState, SelectionIndex) -> SelectionIndex) {
        _state.update { state ->
            val navIdx  = state.currentNavIndex
            val old     = state.selections.getOrElse(navIdx) { SelectionIndex(navIdx, 0, 0) }
            val new     = transform(state, old)
            val updated = state.selections.toMutableList()
            if (navIdx < updated.size) updated[navIdx] = new
            else {
                while (updated.size < navIdx) updated.add(SelectionIndex(updated.size, 0, 0))
                updated.add(new)
            }
            state.copy(selections = updated)
        }
        saveDraft()
    }

    private data class Draft(
        val selections: List<SelectionIndex>,
        val currentNavIndex: Int,
        val currentNavIndexChar1: Int,
        val currentNavIndexChar2: Int,
        val activeCharacter: Int,
        val isFlipped: Boolean
    )

    private fun saveDraft() {
        val state = _state.value
        val templateId = state.template?.id ?: return
        savedStateHandle[KEY_DRAFT_TEMPLATE_ID] = templateId
        savedStateHandle[KEY_DRAFT_SELECTIONS] = ArrayList(state.selections)
        savedStateHandle[KEY_DRAFT_NAV_INDEX] = state.currentNavIndex
        savedStateHandle[KEY_DRAFT_NAV_INDEX_CHAR1] = state.currentNavIndexChar1
        savedStateHandle[KEY_DRAFT_NAV_INDEX_CHAR2] = state.currentNavIndexChar2
        savedStateHandle[KEY_DRAFT_ACTIVE_CHARACTER] = state.activeCharacter
        savedStateHandle[KEY_DRAFT_FLIPPED] = state.isFlipped
    }

    private fun restoreDraft(templateId: String, parts: List<BodyPartModel>): Draft? {
        if (savedStateHandle.get<String>(KEY_DRAFT_TEMPLATE_ID) != templateId) return null
        val selections = savedStateHandle
            .get<ArrayList<SelectionIndex>>(KEY_DRAFT_SELECTIONS)
            ?.let { clampSelections(parts, it) }
            ?: return null
        return Draft(
            selections = selections,
            currentNavIndex = savedStateHandle.get<Int>(KEY_DRAFT_NAV_INDEX)
                ?.coerceIn(0, maxOf(0, parts.lastIndex)) ?: 0,
            currentNavIndexChar1 = savedStateHandle.get<Int>(KEY_DRAFT_NAV_INDEX_CHAR1)
                ?.coerceIn(0, maxOf(0, parts.lastIndex)) ?: firstNavIndexForChar(parts, 1),
            currentNavIndexChar2 = savedStateHandle.get<Int>(KEY_DRAFT_NAV_INDEX_CHAR2)
                ?.coerceIn(0, maxOf(0, parts.lastIndex)) ?: firstNavIndexForChar(parts, 2),
            activeCharacter = savedStateHandle.get<Int>(KEY_DRAFT_ACTIVE_CHARACTER)
                ?.takeIf { it == 1 || it == 2 } ?: 1,
            isFlipped = savedStateHandle.get<Boolean>(KEY_DRAFT_FLIPPED) ?: false
        )
    }

    private fun firstNavIndexForChar(parts: List<BodyPartModel>, charType: Int): Int {
        return parts.indexOfFirst { it.charType == charType }.takeIf { it >= 0 } ?: 0
    }

    private fun clampNavIndex(parts: List<BodyPartModel>, charType: Int, navIndex: Int): Int {
        val firstIndex = firstNavIndexForChar(parts, charType)
        if (parts.getOrNull(navIndex)?.charType == charType) return navIndex
        return firstIndex
    }

    private companion object {
        const val KEY_DRAFT_TEMPLATE_ID = "customize_template_id"
        const val KEY_DRAFT_SELECTIONS = "customize_selections"
        const val KEY_DRAFT_NAV_INDEX = "customize_nav_index"
        const val KEY_DRAFT_NAV_INDEX_CHAR1 = "customize_nav_index_char1"
        const val KEY_DRAFT_NAV_INDEX_CHAR2 = "customize_nav_index_char2"
        const val KEY_DRAFT_ACTIVE_CHARACTER = "customize_active_character"
        const val KEY_DRAFT_FLIPPED = "customize_flipped"
    }
}
