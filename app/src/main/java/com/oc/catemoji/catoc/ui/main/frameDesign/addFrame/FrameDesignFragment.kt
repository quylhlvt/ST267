package com.oc.catemoji.catoc.ui.main.frameDesign.addFrame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.onClick
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.core.extention.setTextActionBar
import com.oc.catemoji.catoc.databinding.FragmentFrameDesignBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FrameDesignFragment : BaseFragment<FragmentFrameDesignBinding, FrameDesignViewModel>(
    FragmentFrameDesignBinding::inflate, FrameDesignViewModel::class.java
){
    private val frameAdapter by lazy {
        FrameAssetAdapter { framePath ->
            navController?.navigate(
                R.id.addOneFrameFragment,
                Bundle().apply {
                    putString("framePath", framePath)
                }
            )
        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick {
            navController?.popBackStack()
        }
    }

    override fun initView() {
        super.initView()
        setupActionBar()
        setupRecyclerView()
    }

    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setTextActionBar(tvCenter, getString(R.string.my_creation1))
        }
    }

    private fun setupRecyclerView() {
        binding.recycleFrameCouple.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = frameAdapter
        }
        frameAdapter.submitList(getFramePathsFromAssets())
    }

    private fun getFramePathsFromAssets(): List<String> {
        return requireContext().assets
            .list("listFrame")
            ?.filter { it.endsWith(".webp", true) }
            ?.sortedBy { it.substringBeforeLast(".").toIntOrNull() ?: Int.MAX_VALUE }
            ?.map { "listFrame/$it" }
            ?: emptyList()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentFrameDesignBinding = FragmentFrameDesignBinding.inflate(inflater, container, false)

    override fun bindViewModel() {

    }
}
