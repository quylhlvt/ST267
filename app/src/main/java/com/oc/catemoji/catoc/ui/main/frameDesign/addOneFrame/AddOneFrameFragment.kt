package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.onClick
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.data.model.frameDesign.CropImage
import com.oc.catemoji.catoc.databinding.FragmentAddOneFrameBinding
import com.oc.catemoji.catoc.ui.main.frameDesign.scale.FrameDetector
import com.oc.catemoji.catoc.ui.main.frameDesign.scale.FrameMultiRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@registerForActivityResult

            runCatching {
                requireActivity().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val path = uri.toString()
            galleryImages.remove(path)
            galleryImages.add(0, path)
            galleryAdapter.submitList(galleryImages.toList())
            setSelectedImage(path)
            showGalleryTab()
        }

    override fun initView() {
        setupScaleGesture()
        setupActionBar()
        setupRecyclerView()
        loadFrame(
            arguments?.getString("framePath")
                ?: "listFrame/${arguments?.getInt("frameNumber") ?: 1}.webp"
        )
        viewModel.loadAvatarList()
    }

    override fun onFragmentStart() {
        viewModel.loadAvatarList()
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

            renderFrame()
            setupFrameClick()
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
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                rotation = 0f
            )
            renderFrame()
        }
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
            openGalleryPicker()
        }
    }
    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.avatarList.collect { list ->
                    avatarAdapter.submitList(list.map { it.path })
                }
            }
        }
    }

    private fun showAvatarTab() {
        binding.recyclerViewCustom.isVisible = true
        binding.recyclerViewGallery.isVisible = false
        binding.imvFocusMyCouple.setImageResource(com.oc.catemoji.catoc.R.drawable.bg_btn_type_selected_frame)
        binding.imvFocusGallery.setImageDrawable(null)
        binding.tvMyCouple.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.app_color))
        binding.tvGallery.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.white))
    }

    private fun showGalleryTab() {
        binding.recyclerViewCustom.isVisible = false
        binding.recyclerViewGallery.isVisible = true
        binding.imvFocusMyCouple.setImageDrawable(null)
        binding.imvFocusGallery.setImageResource(com.oc.catemoji.catoc.R.drawable.bg_btn_type_selected_frame)
        binding.tvMyCouple.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.white))
        binding.tvGallery.setTextColor(requireContext().getColor(com.oc.catemoji.catoc.R.color.app_color))
    }

    private fun openGalleryPicker() {
        imagePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentAddOneFrameBinding = FragmentAddOneFrameBinding.inflate(inflater, container, false)

    override fun bindViewModel() {

    }

}
