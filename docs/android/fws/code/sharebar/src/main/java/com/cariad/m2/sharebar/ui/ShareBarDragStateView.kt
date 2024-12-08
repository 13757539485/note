package com.cariad.m2.sharebar.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.cariad.m2.sharebar.R
import com.cariad.m2.sharebar.databinding.ItemShareAppBinding

/**
 * 拖拽切换状态view
 */
class ShareBarDragStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: ItemShareAppBinding

    init {
        binding = ItemShareAppBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setItemName(text: CharSequence) {
        binding.itemName.text = text
    }

    fun setItemIcon(icon: Drawable) {
        binding.itemIcon.setImageDrawable(icon)
    }

    fun registerDragListener(block: (event: DragEvent) -> Unit) {
        setOnDragListener { v, event ->
            block.invoke(event)
            if (event.action == DragEvent.ACTION_DRAG_ENTERED) {
//                binding.itemBg.setBackgroundResource(R.drawable.share_item_bg_hover)
                binding.itemOverlay.isVisible = true
                binding.itemName.ellipsize = TextUtils.TruncateAt.MARQUEE
                binding.itemName.isSelected = true
            } else if (event.action == DragEvent.ACTION_DRAG_EXITED ||
                event.action == DragEvent.ACTION_DRAG_ENDED) {
//                binding.itemBg.setBackgroundResource(android.R.color.transparent)
                binding.itemOverlay.isVisible = false
                binding.itemName.ellipsize = TextUtils.TruncateAt.END
                binding.itemName.isSelected = false
            }
            true
        }
    }
}
