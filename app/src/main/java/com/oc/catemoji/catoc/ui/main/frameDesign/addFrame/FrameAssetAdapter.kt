package com.oc.catemoji.catoc.ui.main.frameDesign.addFrame

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerDrawable
import com.oc.catemoji.catoc.core.extention.gone
import com.oc.catemoji.catoc.databinding.ItemFrameDesignBinding
import com.oc.catemoji.catoc.utils.DataLocal

class FrameAssetAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<FrameAssetAdapter.FrameViewHolder>() {

    private var items: List<String> = emptyList()

    fun submitList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class FrameViewHolder(
        private val binding: ItemFrameDesignBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(path: String) = binding.apply {
            val shimmerDrawable = ShimmerDrawable().apply { setShimmer(DataLocal.shimmer) }

            btnDelete.gone()
            btnSelect.gone()
            shadownForcus.gone()

            val imageSource = if (path.startsWith("http")) {
                path
            } else {
                "file:///android_asset/$path"
            }

            Glide.with(imvImage)
                .load(imageSource)
                .centerCrop()
                .placeholder(shimmerDrawable)
                .into(imvImage)

            root.setOnClickListener {
                onItemClick(path)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val binding = ItemFrameDesignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FrameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
