package com.oc.catemoji.catoc.ui.onboarding.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.ViewModelActivity
import com.oc.catemoji.catoc.core.base.BackPressHandler
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.dpToPx
import com.oc.catemoji.catoc.core.extention.toIntro
import com.oc.catemoji.catoc.core.extention.toLanguage
import com.oc.catemoji.catoc.core.helper.SharedPreferencesManager.isLanuageScreen
import com.oc.catemoji.catoc.databinding.FragmentSplashBinding
import com.tencent.mmkv.MMKV
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding, SplashViewModel>(
    FragmentSplashBinding::inflate,
    SplashViewModel::class.java
), BackPressHandler {
    private val mainViewModel: ViewModelActivity by activityViewModels()

    private var hasNavigated = false

    companion object {
        private const val MIN_SPLASH_MS = 3_000L
        private const val API_TIMEOUT_MS = 8_000L

    }


    override fun initView() {
        ResourcesCompat.getFont(requireContext(), R.font.baloo2_extrabold)

        checkAndClearDataIfNewVersion()


    }

    private fun checkAndClearDataIfNewVersion() {
        val context = requireContext()
        val currentVersion = context.packageManager
            .getPackageInfo(context.packageName, 0).versionCode
        val savedVersion = sharedPreferences.getVersionCode()

        if (savedVersion != currentVersion) {
            MMKV.defaultMMKV().clearAll()
            sharedPreferences.clearAll()
            context.filesDir.deleteRecursively()
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()

            sharedPreferences.setVersionCode(currentVersion)

            viewLifecycleOwner.lifecycleScope.launch {
                mainViewModel.forceReloadAll()
            }
        }
    }

    override fun viewListener() {}

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.readyToNavigate.collect {
                    if (!hasNavigated) {
                       goToHome()
                    }
                }
            }
        }
    }

    override fun bindViewModel() {}

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)




    private fun goToHome() {
        if (hasNavigated) return
        if (!isAdded || isDetached || isRemoving) return
        hasNavigated = true

        if (!isLanuageScreen()) {
            toLanguage(); return
        }
        toIntro()
    }


    private fun isNetworkAvailable(): Boolean = try {
        val cm = requireContext().getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    } catch (e: Exception) {
        false
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        binding.root.post {
            if (!isAdded || isDetached || isRemoving) return@post

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.startSplashTimer(
                    isOnline = isNetworkAvailable(),
                    waitForOnline = {
                        mainViewModel.templates.first { list ->
                            list.any { it.id.startsWith("online_") }
                        }
                    },
                    waitForImages = {
                        mainViewModel.imagesReady.first { it }
                    }
                )
            }

            AsyncLayoutInflater(requireContext()).inflate(
                R.layout.fragment_home,
                null
            ) { _, _, _ -> }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    override fun onBackPressed(): Boolean {
        return true
    }
}