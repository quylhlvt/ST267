package com.oc.catemoji.catoc.ui.main.frameDesign.addOneFrame

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerDrawable
import com.oc.catemoji.catoc.databinding.ItemImageBinding
import com.oc.catemoji.catoc.utils.DataLocal
import java.io.File

class ImageOneFrameAdapter(
    private var listImage: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ImageOneFrameAdapter.ImageViewHolder>() {

    fun submitList(newList: List<String>) {
        listImage = newList
        notifyDataSetChanged()
    }

    inner class ImageViewHolder(
        private val binding: ItemImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(path: String) {
            val shimmerDrawable = ShimmerDrawable().apply { setShimmer(DataLocal.shimmer) }

            val model: Any = when {
                path.startsWith("content://") || path.startsWith("file://") -> Uri.parse(path)
                File(path).exists() -> File(path)
                else -> "file:///android_asset/$path"
            }

            Glide.with(binding.imvImage)
                .load(model)
                .centerCrop()
                .placeholder(shimmerDrawable)
                .into(binding.imvImage)

            binding.root.setOnClickListener {
                onClick(path)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {

        val binding = ItemImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        holder.bind(listImage[position])
    }

    override fun getItemCount(): Int =
        listImage.size
}
