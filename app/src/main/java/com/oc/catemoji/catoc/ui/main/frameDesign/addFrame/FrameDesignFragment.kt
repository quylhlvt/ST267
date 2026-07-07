package com.oc.catemoji.catoc.ui.main.frameDesign.addFrame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.oc.catemoji.catoc.ViewModelActivity
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.databinding.FragmentFrameDesignBinding
import com.oc.catemoji.catoc.ui.main.createPony.ChoosePonyAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class FrameDesignFragment : BaseFragment<FragmentFrameDesignBinding, FrameDesignViewModel>(
    FragmentFrameDesignBinding::inflate, FrameDesignViewModel::class.java
){
    private val mainViewModel: ViewModelActivity by activityViewModels()
    private lateinit var adapter: ChoosePonyAdapter
    override fun viewListener() {

    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentFrameDesignBinding = FragmentFrameDesignBinding.inflate(inflater, container, false)

    override fun bindViewModel() {

    }
}