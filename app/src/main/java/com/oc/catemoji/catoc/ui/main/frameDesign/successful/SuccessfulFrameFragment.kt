package com.oc.catemoji.catoc.ui.main.frameDesign.successful

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.databinding.FragmentAddOneFrameBinding
import com.oc.catemoji.catoc.databinding.FragmentSuccessfulFrameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuccessfulFrameFragment: BaseFragment<FragmentSuccessfulFrameBinding, SuccessfulFrameViewModel>(
    FragmentSuccessfulFrameBinding::inflate, SuccessfulFrameViewModel::class.java
) {


    override fun viewListener() {

    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSuccessfulFrameBinding = FragmentSuccessfulFrameBinding.inflate(inflater, container, false)

    override fun bindViewModel() {

    }



}