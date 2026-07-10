package com.oc.catemoji.catoc.ui.main.myPony

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.dialog.CreateNameDialog
import com.oc.catemoji.catoc.core.extention.InternetExtension.isNetworkConnected
import com.oc.catemoji.catoc.core.extention.checkPermissions
import com.oc.catemoji.catoc.core.extention.goToSettings
import com.oc.catemoji.catoc.core.extention.gone
import com.oc.catemoji.catoc.core.extention.invisible
import com.oc.catemoji.catoc.core.extention.onClick
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.core.extention.setTextActionBar
import com.oc.catemoji.catoc.core.extention.toCleanSelections
import com.oc.catemoji.catoc.core.extention.visible
import com.oc.catemoji.catoc.core.helper.PermissionRequestHelper
import com.oc.catemoji.catoc.data.model.mypony.MyAlbumModel
import com.oc.catemoji.catoc.databinding.FragmentMyPonyBinding
import com.oc.catemoji.catoc.ui.main.customize.CustomizeFragment
import com.oc.catemoji.catoc.ui.main.myPony.adapter.FrameDesignAdapter
import com.oc.catemoji.catoc.ui.main.myPony.adapter.MyAvatarAdapter
import com.oc.catemoji.catoc.ui.main.myPony.adapter.MyDesignAdapter
import com.oc.catemoji.catoc.ui.onboarding.permission.PermissionViewModel
import com.oc.catemoji.catoc.utils.BlockableFrameLayout
import com.oc.catemoji.catoc.utils.share.whatsapp.WhatsappSharingFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class MyPonyFragment : WhatsappSharingFragment<FragmentMyPonyBinding, MyPonyViewModel>(
    FragmentMyPonyBinding::inflate, MyPonyViewModel::class.java
) {
    private val storageHelper = PermissionRequestHelper()

    private lateinit var myAvatarAdapter: MyAvatarAdapter
    private lateinit var myDesignAdapter: MyDesignAdapter
    private lateinit var frameDesignAdapter: FrameDesignAdapter
    private var pendingDownloadPaths: ArrayList<String> = arrayListOf()
    private val permissionViewModel: PermissionViewModel by activityViewModels()

    private val currentTab = MutableStateFlow(MyPonyTab.AVATAR)

    private enum class MyPonyTab {
        AVATAR, DESIGN, FRAME
    }

    companion object {
        private const val ADD_PACK_REQUEST = 200
        private const val MIN_STICKERS_WHATSAPP = 3
        private const val MAX_STICKERS_WHATSAPP = 30
    }

    private fun performBatchDownload() {
        viewModel.downloadFiles(requireContext(), pendingDownloadPaths)
        resetSelection()
    }

    // ── INIT ──────────────────────────────────────────────────────────────────


    override fun onFragmentStart() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = false
    }

    override fun onFragmentStop() {
        if (!isAdded || isDetached) return
        (binding.flNativeCollab as? BlockableFrameLayout)?.isBlocked = true
        binding.flNativeCollab.removeAllViews()
    }

    override fun initView() {


        binding.apply {
            tvWhatApp.isSelected = true
            tvTelegram.isSelected = true
            tvShare.isSelected = true
            tvDownload.isSelected = true
        }
        setupActionBar()
        setupTabs()
        setupRecyclerViews()
        setupBottomButtons()
        setupTouchListenerForResetSelection()
        loadAvatarData()
    }

    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setTextActionBar(tvCenter, getString(R.string.my_creation1))
            setImageActionBar(btnActionBarNextToRight, R.drawable.ic_delete_all)
            setImageActionBar(btnActionBarRight, R.drawable.ic_select_all)
            btnActionBarNextToRight.invisible()
            btnActionBarRight.invisible()
        }
    }

    private fun setupTabs() {
        binding.btnMyAvatar.onClick { switchTab(MyPonyTab.AVATAR) }
        binding.btnMyDesign.onClick { switchTab(MyPonyTab.DESIGN) }
        binding.btnFrameDesign.onClick { switchTab(MyPonyTab.FRAME) }
    }

    private fun switchTab(tab: MyPonyTab) {
        currentTab.value = tab
        applyTabUI(tab)
        resetSelection()
    }

    private fun applyTabUI(tab: MyPonyTab) {
        binding.apply {
            val selectedTextColor = requireContext().getColor(R.color.white)
            val unselectedTextColor = requireContext().getColor(R.color.app_color)

            imvFocusMyAvatar.setImageResource(
                if (tab == MyPonyTab.AVATAR) R.drawable.bg_btn_type_selected else R.drawable.bg_btn_type_unselected
            )
            imvFocusMyDesign.setImageResource(
                if (tab == MyPonyTab.DESIGN) R.drawable.bg_btn_type_selected else R.drawable.bg_btn_type_unselected
            )
            imvFocusFrameDesign.setImageResource(
                if (tab == MyPonyTab.FRAME) R.drawable.bg_btn_type_selected else R.drawable.bg_btn_type_unselected
            )

            tvMyAvatar.setTextColor(if (tab == MyPonyTab.AVATAR) selectedTextColor else unselectedTextColor)
            tvMyDesign.setTextColor(if (tab == MyPonyTab.DESIGN) selectedTextColor else unselectedTextColor)
            tvFrameDesign.setTextColor(if (tab == MyPonyTab.FRAME) selectedTextColor else unselectedTextColor)

            recycleAvatar.isVisible = tab == MyPonyTab.AVATAR
            recycleDesign.isVisible = tab == MyPonyTab.DESIGN
            recycleFrameDesign.isVisible = tab == MyPonyTab.FRAME

            when (tab) {
                MyPonyTab.AVATAR -> updateEmptyState(myAvatarAdapter.items.isEmpty())
                MyPonyTab.DESIGN -> {
                    updateEmptyState(myDesignAdapter.items.isEmpty())
                    loadDesignData()
                }
                MyPonyTab.FRAME -> {
                    updateEmptyState(frameDesignAdapter.items.isEmpty())
                    loadFrameDesignData()
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        myAvatarAdapter = MyAvatarAdapter(requireContext()).apply {
            onItemClick = { item ->
                    handleItemClick(item.path, true, 1, item.idEdit)

            }
            onLongClick = { position -> handleLongClick(position, MyPonyTab.AVATAR) }
            onItemTick = { position -> toggleSelection(position, MyPonyTab.AVATAR) }
            onEditClick = { idEdit ->
                if (ensureEditItemExists(idEdit)) {
                    navigateToEdit(idEdit)
                }
            }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), MyPonyTab.AVATAR) }
        }
        binding.recycleAvatar.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = myAvatarAdapter
            itemAnimator = null
        }

        myDesignAdapter = MyDesignAdapter().apply {
            onItemClick = { path ->    handleItemClick(path, false, 2, "0") }
            onLongClick = { position -> handleLongClick(position, MyPonyTab.DESIGN) }
            onItemTick = { position -> toggleSelection(position, MyPonyTab.DESIGN) }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), MyPonyTab.DESIGN) }
        }
        binding.recycleDesign.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = myDesignAdapter
            itemAnimator = null
        }

        frameDesignAdapter = FrameDesignAdapter().apply {
            onItemClick = { path -> handleItemClick(path, false, 3, "0") }
            onLongClick = { position -> handleLongClick(position, MyPonyTab.FRAME) }
            onItemTick = { position -> toggleSelection(position, MyPonyTab.FRAME) }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), MyPonyTab.FRAME) }
        }
        binding.recycleFrameDesign.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = frameDesignAdapter
            itemAnimator = null
        }
    }
    private fun ensureEditItemExists(idEdit: String): Boolean {
        val exists = idEdit.isNotBlank() &&
                viewModelActivity.customizedCharacters.value.any { it.id == idEdit }
        if (!exists) showEditItemNotFoundDialog()
        return exists
    }
    private fun showEditItemNotFoundDialog() {
        showOkDialog(
            title = getString(R.string.error),
            message = getString(R.string.errorcontent)
        )
    }
    private fun setupBottomButtons() {
        binding.apply {
            btnWhatsapp.onClick { handleWhatsAppShare() }
            btnTelegram.onClick { handleTelegramShare() }
            btnDownload.onClick { handleDownload() }
            btnShare.onClick { handleShare() }
            actionBar.btnActionBarRight.onClick { handleSelectAll() }
            actionBar.btnActionBarNextToRight.onClick { handleDeleteSelected() }  // ✅ thêm

        }
    }

    private fun handleShare() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        val paths = selected.map { it.path }.filter { it.isNotEmpty() }
        if (paths.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }

        val uris = ArrayList(paths.map { path ->
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.provider", java.io.File(path)
            )
        })

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share)))
        resetSelection()
    }

    private fun setupTouchListenerForResetSelection() {
        val touchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP && rv.findChildViewUnder(e.x, e.y) == null) {
                    resetSelection()
                    return true
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        }
        binding.recycleAvatar.addOnItemTouchListener(touchListener)
        binding.recycleDesign.addOnItemTouchListener(touchListener)
        binding.recycleFrameDesign.addOnItemTouchListener(touchListener)
    }

    // ── OBSERVE ───────────────────────────────────────────────────────────────
    override fun observeData() {
        // ✅ Chỉ dùng 1 nguồn duy nhất cho avatar
        viewLifecycleOwner.lifecycleScope.launch {
            viewModelActivity.customizedCharacters.collect { customized ->
                val list = customized.filter {
                        it.imageSave.isNotEmpty() && File(it.imageSave).exists()
                    }.sortedByDescending { it.createdAt } // ← dùng createdAt đã fix
                    .map { MyAlbumModel(path = it.imageSave, idEdit = it.id, type = 1) }

                myAvatarAdapter.submitList(list)
                if (currentTab.value == MyPonyTab.AVATAR) updateEmptyState(list.isEmpty())
                updateSelectionUI()
            }
        }

        // Design giữ nguyên
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myDesignList.collect { list ->
                myDesignAdapter.submitList(list)
                if (currentTab.value == MyPonyTab.DESIGN) updateEmptyState(list.isEmpty())
                updateSelectionUI()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myFrameDesignList.collect { list ->
                frameDesignAdapter.submitList(list)
                if (currentTab.value == MyPonyTab.FRAME) updateEmptyState(list.isEmpty())
                updateSelectionUI()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadState.collect { state ->
                when (state) {
                    MyPonyViewModel.DownloadState.SUCCESS -> showToast(
                        getString(
                            R.string.download_success, getString(R.string.app_name)
                        )
                    )

                    MyPonyViewModel.DownloadState.ERROR -> showToast(R.string.download_failed_please_try_again_later)

                    else -> {}
                }
            }
        }
    }

    // ── UI HELPERS ────────────────────────────────────────────────────────────

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.noItem.isVisible = isEmpty
    }

    private fun updateSelectionUI() {
        val currentList = getCurrentItems()
        val hasSelection = currentList.any { it.isShowSelection }
        val allSelected = currentList.isNotEmpty() && currentList.all { it.isSelected }
        val selectedCount = currentList.count { it.isSelected }

        binding.actionBar.apply {
            if (hasSelection) {
                btnActionBarNextToRight.visible()
                btnActionBarRight.visible()

                btnActionBarRight.setImageResource(
                    if (allSelected) R.drawable.ic_select_all else R.drawable.ic_not_select_all
                )
            } else {
                btnActionBarNextToRight.invisible()
                btnActionBarRight.invisible()

            }
        }

        if (hasSelection) {
            binding.lnlBottom.visible()
            if (currentTab.value == MyPonyTab.AVATAR) {
                binding.lnlBottomTop.visible()   // WhatsApp + Telegram
                binding.llBottom.gone()
            } else {
                binding.lnlBottomTop.gone()       // Ẩn WhatsApp + Telegram cho Design tab
                binding.llBottom.visible()

            }
        } else {
            binding.lnlBottom.gone()
        }
    }

    // ── DATA LOADING ──────────────────────────────────────────────────────────

    private fun loadAvatarData() = viewModel.loadMyAvatar(requireContext(), true)
    private fun loadDesignData() = viewModel.loadMyDesign(requireContext())
    private fun loadFrameDesignData() = viewModel.loadMyFrameDesign(requireContext())

    // ── SELECTION ─────────────────────────────────────────────────────────────

    private fun handleItemClick(path: String, isAvatar: Boolean, type: Int, idEdit: String) {
//        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
//        if (currentList.any { it.isShowSelection }) {
//            val position = currentList.indexOfFirst { it.path == path }
//            if (position >= 0) toggleSelection(position, isAvatar)
//        } else {
//            navigateToView(path, type, idEdit)
//        }
        navigateToView(path, type, idEdit)
    }
// MyAvatarAdapter — long click gọi về Fragment


    // Fragment nhận và gọi ViewModel

    private fun handleLongClick(position: Int, tab: MyPonyTab) {
        val currentList = getItems(tab)
        val updatedList = currentList.mapIndexed { index, item ->
            if (index == position) {
                item.copy(isSelected = true, isShowSelection = true)
            } else {
                item.copy(isShowSelection = true)
            }
        }
        submitItems(tab, updatedList)
        setRecyclerBottomMargin(getRecyclerView(tab), 50)
        updateSelectionUI()
    }

    private fun setRecyclerBottomMargin(view: RecyclerView, dpValue: Int) {
        val px = (dpValue * resources.displayMetrics.density).toInt()
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            bottomMargin = px
            view.layoutParams = this
        }
    }

    private fun toggleSelection(position: Int, tab: MyPonyTab) {
        val currentList = getItems(tab)
        val updatedList = currentList.mapIndexed { index, item ->
            if (index == position) item.copy(isSelected = !item.isSelected) else item
        }
        submitItems(tab, updatedList)

        updateSelectionUI()
        if (updatedList.none { it.isSelected }) resetSelection()
    }

    private fun handleDeleteSelected() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        val paths = ArrayList(selected.map { it.path })
        confirmDelete(paths, currentTab.value)
    }

    private fun handleSelectAll() {
        val currentList = getCurrentItems()
        val shouldSelectAll = !currentList.all { it.isSelected }
        val updatedList =
            currentList.map { it.copy(isSelected = shouldSelectAll, isShowSelection = true) }
        submitItems(currentTab.value, updatedList)
        updateSelectionUI()
    }

    private fun resetSelection() {
        val avatarReset =
            myAvatarAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        val designReset =
            myDesignAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        val frameReset =
            frameDesignAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        myAvatarAdapter.submitList(avatarReset)
        myDesignAdapter.submitList(designReset)
        frameDesignAdapter.submitList(frameReset)

        // Reset margin về 0
        setRecyclerBottomMargin(binding.recycleAvatar, 0)
        setRecyclerBottomMargin(binding.recycleDesign, 0)
        setRecyclerBottomMargin(binding.recycleFrameDesign, 0)

        updateSelectionUI()
    }

    private fun getSelectedItems(): List<MyAlbumModel> =
        getCurrentItems().filter { it.isSelected }

    private fun getCurrentItems(): List<MyAlbumModel> = getItems(currentTab.value)

    private fun getItems(tab: MyPonyTab): List<MyAlbumModel> = when (tab) {
        MyPonyTab.AVATAR -> myAvatarAdapter.items
        MyPonyTab.DESIGN -> myDesignAdapter.items
        MyPonyTab.FRAME -> frameDesignAdapter.items
    }

    private fun submitItems(tab: MyPonyTab, items: List<MyAlbumModel>) {
        when (tab) {
            MyPonyTab.AVATAR -> myAvatarAdapter.submitList(items)
            MyPonyTab.DESIGN -> myDesignAdapter.submitList(items)
            MyPonyTab.FRAME -> frameDesignAdapter.submitList(items)
        }
    }

    private fun getRecyclerView(tab: MyPonyTab): RecyclerView = when (tab) {
        MyPonyTab.AVATAR -> binding.recycleAvatar
        MyPonyTab.DESIGN -> binding.recycleDesign
        MyPonyTab.FRAME -> binding.recycleFrameDesign
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private fun navigateToView(path: String, type: Int, idEdit: String) {
        val action = MyPonyFragmentDirections.actionMyponyToView(path, idEdit, type)
        findNavController().navigate(action)
    }

    /**
     * Navigate sang CustomizeFragment ở chế độ Edit.
     *
     * Vấn đề: khi save từ template, ViewModelActivity.saveCharacterWithSelections() copy
     * character với id = UUID mới. Không có field "templateId" nào được lưu lại.
     *
     * Giải pháp: dùng [CustomModel.avatar] của customized character để tìm template gốc
     * có cùng avatar (template gốc KHÔNG thay đổi avatar, chỉ customized mới có imageSave riêng).
     *
     * Nếu project có field templateId trong CustomModel thì dùng trực tiếp field đó thay thế.
     */
    private fun navigateToEdit(idEdit: String) {
        val customized =
            viewModelActivity.customizedCharacters.value.firstOrNull { it.id == idEdit }
                ?: run { showToast("Character not found"); return }

        val templateIndex =
            viewModelActivity.getTemplateIndexForCustomized(idEdit).takeIf { it >= 0 }
                ?: run { showUnstableNetworkDialog(); return }

        val template = viewModelActivity.templates.value.getOrNull(templateIndex)

        // ✅ Thêm check: online template + mất mạng hoặc data chưa đủ
        if (template?.id?.startsWith("online_") == true) {
            val onlineTemplateCount =
                viewModelActivity.templates.value.count { it.id.startsWith("online_") }
            if (!isNetworkConnected(requireContext()) || onlineTemplateCount < 2) {
                showUnstableNetworkDialog()
                return
            }
        }

        val args = CustomizeFragment.newArgs(
            templateIndex = templateIndex,
            isEdit = true,
            customizedId = idEdit,
            savedSelections = customized.selections.toCleanSelections(),
            isFlipped = customized.isFlipped
        )
          findNavController().navigate(R.id.action_mypony_to_custom, args)

    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    private fun confirmDelete(paths: ArrayList<String>, tab: MyPonyTab) {
        showConfirmDialog(
            title = getString(R.string.delete),
            message = getString(R.string.are_you_sure_want_to_delete_this_item),
            onYes = {
                when (tab) {
                    MyPonyTab.AVATAR -> viewModel.deleteItem(requireContext(), paths)
                    MyPonyTab.DESIGN -> viewModel.deleteItemDesign(paths, requireContext())
                    MyPonyTab.FRAME -> viewModel.deleteItemFrameDesign(paths, requireContext())
                }
                resetSelection()
            },
            onNo = null
        )
    }

    private fun handleDownload() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        pendingDownloadPaths = ArrayList(selected.map { it.path })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performBatchDownload(); return
        }

        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            requireContext().checkPermissions(arrayOf(permission)) -> performBatchDownload()
            permissionViewModel.shouldGoToSettings(isStorage = true) -> activity?.goToSettings()
            else -> downloadPermissionLauncher.launch(arrayOf(permission))
        }
    }

    private val downloadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                permissionViewModel.onStorageGranted()
                performBatchDownload()
            } else {
                permissionViewModel.onStorageDenied()
                // ✅ Chỉ toast, KHÔNG check goToSettings ở đây
                showToast(R.string.download_failed_please_try_again_later)
            }
        }
    // ── SHARE: chỉ dùng imageSave (ảnh render) ───────────────────────────────

    /**
     * Lấy đúng path để share.
     * - Avatar tab: dùng [MyAlbumModel.path] = customized.imageSave (ảnh render đã lưu)
     * - Design tab: dùng path trực tiếp
     * KHÔNG dùng customized.avatar (đó là thumbnail template gốc từ assets)
     */
    private fun getSharePaths(): List<String> =
        getSelectedItems().map { it.path }.filter { it.isNotEmpty() }

    // ── WHATSAPP ──────────────────────────────────────────────────────────────

    private fun handleWhatsAppShare() {
        val paths = getSharePaths()
        when {
            paths.isEmpty() -> {
                showToast(R.string.please_select_an_image); return
            }

            paths.size < MIN_STICKERS_WHATSAPP -> {
                showToast(R.string.limit_3_items); return
            }

            paths.size > MAX_STICKERS_WHATSAPP -> {
                showToast(R.string.limit_30_items); return
            }
        }
        // ✅ Dùng CreateNameDialog thay AlertDialog
        val dialog = CreateNameDialog(requireActivity())
        dialog.show()
        dialog.onYesClick = { packName ->
            dialog.dismiss()
            viewModel.addToWhatsapp(requireContext(), packName, ArrayList(paths)) { pack ->
                if (pack != null) {
                    addToWhatsapp(pack)
                    resetSelection()
                } else showToast("Failed to create sticker pack")
            }
        }
        dialog.onNoClick = { dialog.dismiss() }
        dialog.onDismissClick = { dialog.dismiss() }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ADD_PACK_REQUEST) return
        when (resultCode) {
            android.app.Activity.RESULT_OK -> showToast("Sticker pack added successfully")
            android.app.Activity.RESULT_CANCELED -> {
                val err = data?.getStringExtra("validation_error")
                if (err != null) {
                    Log.e("MyPonyFragment", "Validation: $err"); showToast("Failed: $err")
                } else showToast("Cancelled")
            }
        }
    }

    // ── TELEGRAM ──────────────────────────────────────────────────────────────

    private fun handleTelegramShare() {
        val paths = getSharePaths()
        if (paths.isEmpty()) {
            showToast(R.string.please_select_an_image); return
        }
        viewModel.addToTelegram(requireContext(), ArrayList(paths))
        resetSelection()
    }

    // ── UTILITY ───────────────────────────────────────────────────────────────

    private fun showToast(resId: Int) =
        android.widget.Toast.makeText(requireContext(), resId, android.widget.Toast.LENGTH_SHORT)
            .show()

    private fun showToast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT)
            .show()

    // ── BASE OVERRIDES ────────────────────────────────────────────────────────

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.setOnClickListener {
            if (myAvatarAdapter.items.any { it.isShowSelection } ||
                myDesignAdapter.items.any { it.isShowSelection } ||
                frameDesignAdapter.items.any { it.isShowSelection }
            ) {
                resetSelection()  // Thoát selection mode, KHÔNG navigate
            } else {
                findNavController().navigateUp()
            }
        }
    }

    override fun bindViewModel() {}

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentMyPonyBinding = FragmentMyPonyBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        applyTabUI(currentTab.value)
        when (currentTab.value) {
            MyPonyTab.AVATAR -> Unit
            MyPonyTab.DESIGN -> loadDesignData()
            MyPonyTab.FRAME -> loadFrameDesignData()
        }
    }
}
