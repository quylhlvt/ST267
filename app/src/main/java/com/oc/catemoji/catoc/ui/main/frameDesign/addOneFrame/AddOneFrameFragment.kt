package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import android.content.ContentUris
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.checkPermissions
import com.oc.catemoji.catoc.core.extention.goToSettings
import com.oc.catemoji.catoc.core.extention.onClick
import com.oc.catemoji.catoc.core.extention.requestPermission
import com.oc.catemoji.catoc.core.extention.saveToFile
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.core.helper.PermissionHelper
import com.oc.catemoji.catoc.data.model.frameDesign.CropImage
import com.oc.catemoji.catoc.databinding.FragmentAddOneFrameBinding
import com.oc.catemoji.catoc.ui.main.frameDesign.scale.FrameDetector
import com.oc.catemoji.catoc.ui.main.frameDesign.scale.FrameMultiRenderer
import com.oc.catemoji.catoc.utils.key.IntentKey
import com.oc.catemoji.catoc.utils.key.RequestKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class AddOneFrameFragment : BaseFragment<FragmentAddOneFrameBinding, AddOneFrameViewModel>(
    FragmentAddOneFrameBinding::inflate, AddOneFrameViewModel::class.java
){
    private lateinit var scaleDetector: ScaleGestureDetector

    private val selectedImages = mutableMapOf<Int, CropImage>()
    private var frameBitmap: Bitmap? = null
    private var targetRects: List<Rect> = emptyList()

    private var selectedIndex = 0
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    private var lastRotationAngle = 0f
    private var isRotating = false

    private val galleryImages = mutableListOf<String>()
    private var currentTab = Tab.MY_CREATION
    private var galleryPermissionDenied = false
    private var isSaving = false

    private val avatarAdapter by lazy {
        ImageOneFrameAdapter(emptyList()) { imagePath ->
            setSelectedImage(imagePath)
        }
    }
    private val galleryAdapter by lazy {
        ImageOneFrameAdapter(emptyList()) { imagePath ->
            setSelectedImage(imagePath)
        }
    }
    private enum class Tab {
        MY_CREATION,
        GALLERY
    }
    override fun initView() {
        binding.apply {
            tvEmptyAction.isSelected = true
        }
        setupScaleGesture()
        setupActionBar()
        setupRecyclerView()
        loadFrame(
            arguments?.getString("framePath")
                ?: "listFrame/${arguments?.getInt("frameNumber") ?: 1}.png"
        )
        viewModel.loadAvatarList()
        showAvatarTab()
    }

    override fun onFragmentStart() {
        viewModel.loadAvatarList()
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == Tab.GALLERY && hasStoragePermission()) {
            loadGalleryImages()
        }
    }

    private fun setupScaleGesture() {
        scaleDetector = ScaleGestureDetector(
            requireActivity(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cropImage = selectedImages[selectedIndex]
                        ?: return true

                    cropImage.scale *= detector.scaleFactor

                    // Cho phép thu nhỏ còn 20%, phóng to tối đa 5 lần
                    cropImage.scale = cropImage.scale.coerceIn(
                        0.5f,
                        1.7f
                    )

                    clampCropImageOffset(cropImage)
                    saveSelectedImageDraft(selectedIndex, cropImage)
                    renderFrame()

                    return true
                }
            }
        )
    }
    private fun loadFrame(framePath: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                val frame = loadBitmapFromAssets(framePath) ?: return@withContext null
                frame to FrameDetector.detectAllTransparentRects(frame)
            } ?: return@launch

            if (!isAdded) return@launch

            frameBitmap = loaded.first
            targetRects = loaded.second
            selectedIndex = -1
            selectedImages.clear()
            restoreSelectedImages()

            renderFrame()
            updateSaveButtonVisibility()
            setupFrameClick()
        }
    }

    private suspend fun restoreSelectedImages() {
        viewModel.getSelectedImageDrafts().forEach { (index, draft) ->
            if (index !in targetRects.indices) return@forEach
            val bitmap = withContext(Dispatchers.Default) {
                loadBitmap(draft.path)
            } ?: return@forEach

            val cropImage = CropImage(
                bitmap = bitmap,
                path = draft.path,
                scale = draft.scale,
                offsetX = draft.offsetX,
                offsetY = draft.offsetY,
                rotation = draft.rotation
            )
            selectedImages[index] = cropImage
            clampCropImageOffset(index, cropImage)
        }
    }
    private fun getRotationAngle(event: MotionEvent): Float {
        if (event.pointerCount < 2) {
            return 0f
        }

        val deltaX =
            event.getX(1) - event.getX(0)

        val deltaY =
            event.getY(1) - event.getY(0)

        return Math.toDegrees(
            kotlin.math.atan2(
                deltaY.toDouble(),
                deltaX.toDouble()
            )
        ).toFloat()
    }
    private fun setupFrameClick() {
        binding.imgeFrame.setOnTouchListener { _, event ->

            scaleDetector.onTouchEvent(event)

            val frame =
                frameBitmap ?: return@setOnTouchListener true

            val imageView = binding.imgeFrame

            val viewScale = minOf(
                imageView.width.toFloat() / frame.width,
                imageView.height.toFloat() / frame.height
            )

            val dx =
                (imageView.width - frame.width * viewScale) / 2f

            val dy =
                (imageView.height - frame.height * viewScale) / 2f

            val bitmapX =
                (event.x - dx) / viewScale

            val bitmapY =
                (event.y - dy) / viewScale

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    val index =
                        targetRects.indexOfFirst {

                            it.contains(
                                bitmapX.toInt(),
                                bitmapY.toInt()
                            )
                        }

                    if (index != -1) {

                        selectedIndex = index

                        lastX = bitmapX
                        lastY = bitmapY

                        isDragging = true

                        renderFrame()
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {

                    if (event.pointerCount >= 2) {

                        lastRotationAngle =
                            getRotationAngle(event)

                        isRotating = true

                        isDragging = false
                    }
                }

                MotionEvent.ACTION_MOVE -> {

                    val cropImage =
                        selectedImages[selectedIndex]

                    if (cropImage != null) {

                        // 2 NGÓN: ZOOM + XOAY
                        if (event.pointerCount >= 2) {

                            val currentAngle =
                                getRotationAngle(event)

                            if (isRotating) {

                                var angleDelta =
                                    currentAngle - lastRotationAngle

                                // Tránh nhảy góc -180 / +180
                                if (angleDelta > 180f) {
                                    angleDelta -= 360f
                                }

                                if (angleDelta < -180f) {
                                    angleDelta += 360f
                                }

                                cropImage.rotation +=
                                    angleDelta

                                clampCropImageOffset(cropImage)
                                saveSelectedImageDraft(selectedIndex, cropImage)

                                lastRotationAngle =
                                    currentAngle

                                renderFrame()
                            }

                        } else if (
                            !scaleDetector.isInProgress &&
                            isDragging
                        ) {

                            // 1 NGÓN: KÉO

                            cropImage.offsetX +=
                                bitmapX - lastX

                            cropImage.offsetY +=
                                bitmapY - lastY

                            clampCropImageOffset(cropImage)
                            saveSelectedImageDraft(selectedIndex, cropImage)

                            lastX = bitmapX
                            lastY = bitmapY

                            renderFrame()
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {

                    isRotating = false

                    if (event.pointerCount - 1 == 1) {
                        isDragging = false
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    isDragging = false
                    isRotating = false
                }
            }

            true
        }
    }
    private fun setupRecyclerView() {
        binding.recyclerViewCustom.apply {
            layoutManager = GridLayoutManager(
                requireContext(),
                4
            )
            adapter = avatarAdapter
        }

        binding.recyclerViewGallery.apply {
            layoutManager = GridLayoutManager(
                requireContext(),
                4
            )
            adapter = galleryAdapter
        }
    }

    private fun setSelectedImage(path: String) {
        val targetIndex = selectedIndex
        if (targetIndex !in targetRects.indices) return

        viewLifecycleOwner.lifecycleScope.launch {
            val photoBitmap = withContext(Dispatchers.Default) {
                loadBitmap(path)
            } ?: return@launch

            if (!isAdded) return@launch

            selectedImages[targetIndex] = CropImage(
                bitmap = photoBitmap,
                path = path,
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                rotation = 0f
            )
            saveSelectedImageDraft(targetIndex, selectedImages[targetIndex] ?: return@launch)
            clampCropImageOffset(targetIndex, selectedImages[targetIndex] ?: return@launch)
            renderFrame()
            updateSaveButtonVisibility()
        }
    }

    private fun saveSelectedImageDraft(
        index: Int,
        cropImage: CropImage
    ) {
        if (cropImage.path.isEmpty()) return
        viewModel.saveSelectedImage(
            index = index,
            path = cropImage.path,
            scale = cropImage.scale,
            offsetX = cropImage.offsetX,
            offsetY = cropImage.offsetY,
            rotation = cropImage.rotation
        )
    }

    private fun clampCropImageOffset(cropImage: CropImage) {
        clampCropImageOffset(selectedIndex, cropImage)
    }

    private fun clampCropImageOffset(
        index: Int,
        cropImage: CropImage
    ) {
        val rect = targetRects.getOrNull(index) ?: return
        val bitmap = cropImage.bitmap

        val baseScale = minOf(
            rect.width().toFloat() / bitmap.width,
            rect.height().toFloat() / bitmap.height
        )
        val finalScale = baseScale * cropImage.scale
        val drawW = bitmap.width * finalScale
        val drawH = bitmap.height * finalScale
        val radians = Math.toRadians(cropImage.rotation.toDouble())
        val halfRotatedW = (
            abs(drawW * cos(radians)) +
                abs(drawH * sin(radians))
            ).toFloat() / 2f
        val halfRotatedH = (
            abs(drawW * sin(radians)) +
                abs(drawH * cos(radians))
            ).toFloat() / 2f

        cropImage.offsetX = clampOffset(
            currentOffset = cropImage.offsetX,
            halfImageSize = halfRotatedW,
            halfFrameSize = rect.width() / 2f
        )
        cropImage.offsetY = clampOffset(
            currentOffset = cropImage.offsetY,
            halfImageSize = halfRotatedH,
            halfFrameSize = rect.height() / 2f
        )
    }

    private fun clampOffset(
        currentOffset: Float,
        halfImageSize: Float,
        halfFrameSize: Float
    ): Float {
        val maxOffset = abs(halfImageSize - halfFrameSize)
        return currentOffset.coerceIn(-maxOffset, maxOffset)
    }

    private fun loadBitmap(path: String): Bitmap? {
        return runCatching {
            when {
                path.startsWith("content://") || path.startsWith("file://") -> {
                    requireContext().contentResolver
                        .openInputStream(Uri.parse(path))
                        ?.use(BitmapFactory::decodeStream)
                }

                File(path).exists() -> BitmapFactory.decodeFile(path)
                else -> loadBitmapFromAssets(path)
            }
        }.getOrNull()
    }

    private fun loadBitmapFromAssets(path: String): Bitmap? {
        return runCatching {
            requireActivity().assets.open(path).use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

    private fun renderFrame() {
        val frame = frameBitmap ?: return

        val result = FrameMultiRenderer.render(
            frame = frame,
            rects = targetRects,
            selectedIndex = selectedIndex,
            selectedImages = selectedImages,
            density = resources.displayMetrics.density
        )

        binding.imgeFrame.setImageBitmap(result)
    }
    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick {
            navController?.popBackStack()
        }

        binding.btnMyCouple.onClick {
            showAvatarTab()
        }

        binding.btnGallery.onClick {
            showGalleryTab()
        }

        binding.btnEmptyAction.onClick {
            when (currentTab) {
                Tab.MY_CREATION -> navController?.navigate(
                    R.id.createPony,
                    Bundle().apply {
                        putBoolean(IntentKey.FROM_ADD_FRAME_CREATION, true)
                    }
                )
                Tab.GALLERY -> requestGalleryPermission()
            }
        }

        binding.actionBar.btnActionBarRightText.onClick {
            performSave()
        }
    }
    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            tvRightText.setText(R.string.save)
            tvRightText.isSelected = true
            btnActionBarRightText.isVisible = false
        }
    }

    private fun updateSaveButtonVisibility() {
        val canSave = targetRects.isNotEmpty() &&
            targetRects.indices.all { selectedImages.containsKey(it) }
        binding.actionBar.btnActionBarRightText.isVisible = canSave && !isSaving
    }

    private fun performSave() {
        if (isSaving || targetRects.isEmpty()) return
        if (!targetRects.indices.all { selectedImages.containsKey(it) }) return

        isSaving = true
        updateSaveButtonVisibility()
        showLoadingSafe()

        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            val density = resources.displayMetrics.density
            val bitmap = withContext(Dispatchers.Default) {
                renderCurrentFrameBitmap(density)
            }
            val savedPath = withContext(Dispatchers.IO) {
                bitmap?.saveToFile(context, "frame")
            }

            if (!isAdded) return@launch

            hideLoadingSafe()
            isSaving = false
            updateSaveButtonVisibility()

            if (savedPath.isNullOrEmpty()) {
                showToast(R.string.download_failed_please_try_again_later)
                return@launch
            }

            withContext(Dispatchers.IO) {
                viewModel.saveFrameDesign(savedPath)
            }

            navController?.navigate(
                R.id.successFragment,
                Bundle().apply {
                    putString("imagePath", savedPath)
                    putInt("imageType", 3)
                }
            )
        }
    }

    private fun renderCurrentFrameBitmap(density: Float): Bitmap? {
        val frame = frameBitmap ?: return null
        return FrameMultiRenderer.render(
            frame = frame,
            rects = targetRects,
            selectedIndex = selectedIndex,
            selectedImages = selectedImages,
            density = density
        )
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.avatarList.collect { list ->
                    val images = list.map { it }
                    avatarAdapter.submitList(images)
                    if (currentTab == Tab.MY_CREATION) {
                        updateAvatarState(images)
                    }
                }
            }
        }
    }

    private fun showAvatarTab() {
        currentTab = Tab.MY_CREATION
        updateAvatarState(viewModel.avatarList.value.map { it })
        binding.recyclerViewGallery.isVisible = false
        binding.imvFocusMyCouple.setImageResource(com.oc.catemoji.catoc.R.drawable.bg_btn_type_selected_frame)
        binding.imvFocusGallery.setImageDrawable(null)
        binding.tvMyCouple.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.app_color))
        binding.tvGallery.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.white))
    }

    private fun showGalleryTab() {
        binding.recyclerViewCustom.isVisible = false
        currentTab = Tab.GALLERY
        binding.imvFocusMyCouple.setImageDrawable(null)
        binding.imvFocusGallery.setImageResource(com.oc.catemoji.catoc.R.drawable.bg_btn_type_selected_frame)
        binding.tvMyCouple.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.white))
        binding.tvGallery.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.app_color))
        if (hasStoragePermission()) {
            loadGalleryImages()
        } else {
            showEmptyState(
                messageRes = R.string.the_app_need_read_storage,
                actionRes = if (galleryPermissionDenied) R.string.go_to_settings else R.string.permission
            )
            binding.recyclerViewGallery.isVisible = false
        }
    }

    private fun updateAvatarState(images: List<String>) {
        binding.recyclerViewCustom.isVisible = images.isNotEmpty()
        binding.emptyState.isVisible = images.isEmpty()
        if (images.isEmpty()) {
            showEmptyState(
                messageRes = R.string.no_item_here,
                actionRes = R.string.create
            )
        }
    }

    private fun showEmptyState(
        messageRes: Int,
        actionRes: Int
    ) {
        binding.emptyState.isVisible = true
        binding.tvEmptyState.setText(messageRes)
        binding.tvEmptyAction.setText(actionRes)
        binding.tvEmptyState.isSelected = true
        binding.tvEmptyAction.isSelected = true
    }

    private fun hasStoragePermission(): Boolean =
        requireContext().checkPermissions(PermissionHelper.storagePermission)

    private fun requestGalleryPermission() {
        if (hasStoragePermission()) {
            loadGalleryImages()
            return
        }
        if (galleryPermissionDenied) {
            activity?.goToSettings()
            return
        }
        requestPermission(
            PermissionHelper.storagePermission,
            RequestKey.STORAGE_PERMISSION_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != RequestKey.STORAGE_PERMISSION_CODE || currentTab != Tab.GALLERY) {
            return
        }

        val granted = grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (granted) {
            galleryPermissionDenied = false
            loadGalleryImages()
        } else {
            galleryPermissionDenied = true
            showEmptyState(
                messageRes = R.string.the_app_need_read_storage,
                actionRes = R.string.go_to_settings
            )
        }
    }

    private fun loadGalleryImages() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contentResolver = requireContext().contentResolver
            val images = withContext(Dispatchers.IO) {
                queryGalleryImages(contentResolver)
            }
            if (!isAdded || currentTab != Tab.GALLERY) return@launch

            galleryImages.clear()
            galleryImages.addAll(images)
            galleryAdapter.submitList(images)
            binding.recyclerViewGallery.isVisible = images.isNotEmpty()
            binding.emptyState.isVisible = images.isEmpty()
            if (images.isEmpty()) {
                showEmptyState(
                    messageRes = R.string.no_item_here,
                    actionRes = R.string.go_to_settings
                )
            }
        }
    }

    private fun queryGalleryImages(contentResolver: ContentResolver): List<String> {
        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val images = mutableListOf<String>()

        contentResolver.query(
            imageCollection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                images.add(ContentUris.withAppendedId(imageCollection, id).toString())
            }
        }

        return images
    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentAddOneFrameBinding = FragmentAddOneFrameBinding.inflate(inflater, container, false)

    override fun bindViewModel() {

    }

}
