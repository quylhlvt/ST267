package com.oc.catemoji.catoc.ui.main.frameDesign.successful

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.checkPermissions
import com.oc.catemoji.catoc.core.extention.goToSettings
import com.oc.catemoji.catoc.core.extention.loadImage
import com.oc.catemoji.catoc.core.extention.onClick
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.core.extention.setTextActionBar
import com.oc.catemoji.catoc.core.extention.visible
import com.oc.catemoji.catoc.databinding.FragmentSuccessfulFrameBinding
import com.oc.catemoji.catoc.ui.main.success.SuccessViewModel
import com.oc.catemoji.catoc.ui.onboarding.permission.PermissionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class SuccessfulFrameFragment : BaseFragment<FragmentSuccessfulFrameBinding, SuccessViewModel>(
    FragmentSuccessfulFrameBinding::inflate, SuccessViewModel::class.java
) {
    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private val imagePathFrame: String by lazy {
        arguments?.getString("imagePathFrame") ?: ""
    }

    override fun initView() {
        binding.apply {
            setImageActionBar(actionBar.btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_share)
            setImageActionBar(actionBar.btnActionBarNextToRight, R.drawable.ic_home)
            setTextActionBar(actionBar.tvCenter, getString(R.string.successful))
            loadImage(requireContext(), imagePathFrame, imvImage)
            txtLeft.apply {
                visible()
                text = getString(R.string.my_creation)
            }
            txtRight.apply {
                visible()
                text = getString(R.string.download)
            }
            tvSuccess.visible()
        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick {
            findNavController().navigateUp()
        }

        binding.actionBar.btnActionBarNextToRight.onClick {
            findNavController().navigate(
                R.id.action_successfulFrameFragment_to_homeFragment
            )
        }

        binding.actionBar.btnActionBarRight.onClick(1500) {
            shareImage()
        }

        binding.btnBottomLeft.onClick {
            findNavController().navigate(
                R.id.action_successfulFrameFragment_to_myPony
            )
        }

        binding.btnBottomRight.onClick {
            downloadImage()
        }
    }

    private fun shareImage() {
        if (imagePathFrame.isEmpty()) return
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            File(imagePathFrame)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun downloadImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performDownload()
            return
        }
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
                showToast(R.string.download_failed_please_try_again_later)
            }
        }

    private fun performDownload() {
        viewModel.downloadFile(requireContext(), imagePathFrame) { success ->
            showToast(
                if (success) {
                    getString(R.string.download_success, getString(R.string.app_name))
                } else {
                    getString(R.string.download_failed_please_try_again_later)
                }
            )
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSuccessfulFrameBinding = FragmentSuccessfulFrameBinding.inflate(inflater, container, false)

    override fun bindViewModel() {
    }
}
