package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.oc.catemoji.catoc.R

class AddOneFrameFragment : Fragment() {

    companion object {
        fun newInstance() = AddOneFrameFragment()
    }

    private val viewModel: AddOneFrameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_one_frame, container, false)
    }
}