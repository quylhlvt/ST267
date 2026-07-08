package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.ViewModelActivity
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.databinding.FragmentAddOneFrameBinding
import com.oc.catemoji.catoc.databinding.FragmentFrameDesignBinding
import com.oc.catemoji.catoc.databinding.FragmentFrameDesignBinding.inflate
import com.oc.catemoji.catoc.ui.main.createPony.ChoosePonyAdapter
import com.oc.catemoji.catoc.ui.main.frameDesign.addFrame.FrameDesignViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class AddOneFrameFragment : BaseFragment<FragmentAddOneFrameBinding, AddOneFrameViewModel>(
    FragmentAddOneFrameBinding::inflate, AddOneFrameViewModel::class.java
){


    override fun viewListener() {

    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentAddOneFrameBinding = FragmentAddOneFrameBinding.inflate(inflater, container, false)

    override fun bindViewModel() {

    }

}