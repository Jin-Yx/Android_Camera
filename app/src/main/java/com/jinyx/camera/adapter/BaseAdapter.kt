package com.jinyx.camera.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.jinyx.camera.bean.ItemEntity

abstract class BaseAdapter(private val itemLayoutMap: Map<Int, Int>) : Adapter<BaseAdapter.BaseViewHolder>() {

    private val itemList: MutableList<ItemEntity> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemLayoutId: Int = itemLayoutMap[viewType]!!
        return BaseViewHolder(LayoutInflater.from(parent.context).inflate(itemLayoutId, parent, false))
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        onItemViewBind(holder, position, itemList[position])
    }

    override fun getItemCount(): Int = itemList.size

    override fun getItemViewType(position: Int): Int = itemList[position].getViewType()

    fun setItem(items: List<ItemEntity>) {
        itemList.clear()
        itemList.addAll(items)
        notifyDataSetChanged()
    }

    abstract fun onItemViewBind(holder: BaseViewHolder, position: Int, item: ItemEntity)

    class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun <T : View> getView(@IdRes vid: Int): T {
            return itemView.findViewById(vid)
        }
    }

}