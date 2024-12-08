package com.cariad.m2.sharebar.ui

import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import com.cariad.m2.sharebar.R
import com.cariad.m2.sharebar.databinding.ItemDragViewBinding
import com.cariad.m2.sharebar.util.PKG_GAODE
import com.cariad.m2.sharebar.util.ShareAppInfo
import com.cariad.m2.sharebar.util.navigate
import com.cariad.m2.sharebar.util.shareApp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

/**
 * 任务岛列表适配器
 */
class ShareBarAdapter: BaseQuickAdapter<ShareAppInfo, ShareBarAdapter.VH>(R.layout.item_drag_view) {
    var dragListener: ((event: DragEvent) -> Unit)? = null

    class VH(
        parent: ViewGroup,
        val binding: ItemDragViewBinding = ItemDragViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : BaseViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun convert(holder: VH, item: ShareAppInfo?) {
        item?.let {
            holder.binding.root.setItemName(it.label)
            holder.binding.root.setItemIcon(it.icon)
        }
        holder.itemView.setOnClickListener {
            item?.let {
                Log.e(
                    "ShareBarAdapter", "onBindViewHolder: " +
                            "${it.packageName} " +
                            "${it.className}  ${it.customUri}" +
                            "${holder.binding.root.layoutParams.width}"
                )
            }
        }
        holder.binding.root.registerDragListener { event ->
            if (event.action == DragEvent.ACTION_DROP) {
                val mimeType = event.clipData.description.getMimeType(0)
                val itemOne = event.clipData.getItemAt(0)
                item?.let {
                    if (it.customUri) {
                        if (it.packageName == PKG_GAODE) {
                            navigate(holder.itemView.context, itemOne.text)
                        }
                    } else {
                        shareApp(
                            holder.itemView.context,
                            mimeType,
                            it.packageName,
                            it.className,
                            itemOne.text,
                            itemOne.uri
                        )
                    }
                }
            }
            dragListener?.invoke(event)
        }
    }
}