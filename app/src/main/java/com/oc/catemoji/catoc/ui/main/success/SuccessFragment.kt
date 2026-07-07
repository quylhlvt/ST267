package com.oc.catemoji.catoc.ui.main.success

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.country.human.countrycup.style.R
import com.country.human.countrycup.style.core.base.BaseFragment
import com.country.human.countrycup.style.core.extention.InternetExtension.isInternetAvailable
import com.country.human.countrycup.style.core.extention.checkPermissions
import com.country.human.countrycup.style.core.extention.goToSettings
import com.country.human.countrycup.style.core.extention.loadImage
import com.country.human.countrycup.style.core.extention.onClick
import com.country.human.countrycup.style.core.extention.safeNavigate
import com.country.human.countrycup.style.core.extention.setImageActionBar
import com.country.human.countrycup.style.core.extention.setTextActionBar
import com.country.human.countrycup.style.core.extention.showInter
import com.country.human.countrycup.style.core.extention.toCleanSelections
import com.country.human.countrycup.style.core.extention.visible
import com.country.human.countrycup.style.core.helper.PermissionRequestHelper
import com.country.human.countrycup.style.databinding.FragmentSuccessBinding
import com.country.human.countrycup.style.ui.main.customize.CustomizeFragment
import com.country.human.countrycup.style.ui.onboarding.permission.PermissionViewModel
import com.lvt.ads.util.Admob
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.InternetExtension.isInternetAvailable
import com.oc.catemoji.catoc.core.extention.checkPermissions
import com.oc.catemoji.catoc.core.extention.goToSettings
import com.oc.catemoji.catoc.core.extention.loadImage
import com.oc.catemoji.catoc.core.extention.onClick
import com.oc.catemoji.catoc.core.extention.safeNavigate
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.core.extention.setTextActionBar
import com.oc.catemoji.catoc.core.extention.toCleanSelections
import com.oc.catemoji.catoc.core.extention.visible
import com.oc.catemoji.catoc.core.helper.PermissionRequestHelper
import com.oc.catemoji.catoc.databinding.FragmentSuccessBinding
import com.oc.catemoji.catoc.ui.main.customize.CustomizeFragment
import com.oc.catemoji.catoc.ui.onboarding.permission.PermissionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.getValue
@AndroidEntryPoint
class SuccessFragment : BaseFragment<FragmentSuccessBinding, SuccessViewModel>(
    FragmentSuccessBinding::inflate,
    SuccessViewModel::class.java
) {
    private val storageHelper = PermissionRequestHelper()

    private val permissionViewModel: PermissionViewModel by activityViewModels()

    private var currentImagePath: String = ""
    private val imagePath: String by lazy { arguments?.getString("imagePath") ?: "" }
    private val imageType: Int    by lazy { arguments?.getInt("imageType", 0) ?: 0 }
    private val idEdit: String    by lazy { arguments?.getString("idEdit") ?: "" }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSuccessBinding = FragmentSuccessBinding.inflate(inflater, container, false)

    override fun initView() {

        currentImagePath = imagePath
        binding.apply {
            setImageActionBar(actionBar.btnActionBarLeft, R.drawable.back_app)
            loadImage(requireContext(), imagePath, imvImage)
            txtLeft.apply  { visible(); text = getString(R.string.my_creation1) }
            txtRight.apply { visible(); text = getString(R.string.download) }
            setTextActionBar(actionBar.tvCenter, getString(R.string.successful))
            setImageActionBar(actionBar.btnActionBarNextToRight, R.drawable.ic_share)
            setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_home)
            tvSuccess.visible()
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick {
                findNavController().navigateUp()
            }

            // Home
            actionBar.btnActionBarRight.onClick {

                    findNavController().navigate(
                        R.id.action_success_to_home, null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true).build()
                    )



            }
            // Share
            actionBar.btnActionBarNextToRight.onClick( 1500) { shareImage() }

            // MyCreation
            btnBottomLeft.onClick {

                    findNavController().navigate(
                        R.id.action_success_to_myPony, null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, false).build()
                    )

            }

            // Download
            btnBottomRight.onClick { downloadImage() }
        }
    }
    private fun shareImage() {
        if (imagePath.isEmpty()) return
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            File(imagePath)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
// ViewFragment.kt

    // Thêm vào ViewFragment
    private fun downloadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { performDownload(); return }
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            requireContext().checkPermissions(arrayOf(permission)) -> performDownload()
            permissionViewModel.shouldGoToSettings(isStorage = true) -> activity?.goToSettings()
            else -> downloadPermissionLauncher.launch(arrayOf(permission))
        }
    }

    private val downloadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                permissionViewModel.onStorageGranted()
                performDownload()
            } else {
                permissionViewModel.onStorageDenied()
                // ✅ Chỉ toast, KHÔNG check goToSettings ở đây
                // goToSettings sẽ được check ở downloadImage() lần nhấn tiếp theo
                showToast(getString(R.string.download_failed_please_try_again_later))
            }
        }
    private fun performDownload() {
        viewModel.downloadFile(requireContext(), imagePath) { success ->
            showToast(
                if (success) getString(R.string.download_success, getString(R.string.app_name))
                else getString(R.string.download_failed_please_try_again_later)
            )
        }
    }

    private fun confirmDelete() {
        showConfirmDialog(
            title = getString(R.string.delete),
            message = getString(R.string.are_you_sure_want_to_delete_this_item),
            onYes = {
                viewModel.deleteFile(
                    path     = imagePath,
                    isAvatar = imageType == 1,
                    idEdit   = idEdit,
                    onDone   = {
                        findNavController().navigateUp()
                    }
                )
            },
            onNo = null
        )
    }
    private fun navigateToEdit() {
        if (idEdit.isEmpty() || imageType != 1) return

        val customized = viewModelActivity.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?: run { showToast("Character not found"); return }

        val templateIndex = viewModelActivity.getTemplateIndexForCustomized(idEdit)
            .takeIf { it >= 0 }
            ?: run { showUnstableNetworkDialog(); return }  // ✅ không tìm thấy template → có thể do chưa load online

        val template = viewModelActivity.templates.value.getOrNull(templateIndex)

        // ✅ Thêm check: online template + mất mạng + online templates < 2
        if (template?.id?.startsWith("online_") == true) {
            val onlineTemplateCount = viewModelActivity.templates.value
                .count { it.id.startsWith("online_") }
            if (!isInternetAvailable(requireContext()) || onlineTemplateCount < 2) {
                showUnstableNetworkDialog()
                return
            }
        }

        val args = CustomizeFragment.newArgs(
            templateIndex   = templateIndex,
            isEdit          = true,
            customizedId    = idEdit,
            savedSelections = customized.selections.toCleanSelections(),
            isFlipped       = customized.isFlipped
        )
        findNavController().safeNavigate(R.id.action_view_to_customize, args)
    }

    private fun showToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun observeData() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("updated_image_path")
            ?.observe(viewLifecycleOwner) { newPath ->
                if (newPath.isNullOrEmpty()) return@observe
                currentImagePath = newPath
                loadImage(requireContext(), currentImagePath, binding.imvImage)
            }
    }
    override fun bindViewModel() {}
}