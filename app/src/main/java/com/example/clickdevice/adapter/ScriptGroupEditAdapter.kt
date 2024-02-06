package com.example.clickdevice.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.clickdevice.databinding.ItemScriptGroupEditBinding
import java.lang.StringBuilder

class ScriptGroupEditAdapter<T> : RecyclerView.Adapter<ViewHolder>() {

    private var mData: List<Item<T>> = emptyList()

    data class Item<T>(val name: String, val msg: String, val data: T)


    var isShowPosition = false
    var btnVisibility = true


    var editCallBack: ((position: Int, Item<T>) -> Unit)? = null
    var deleteCallBack: ((position: Int, Item<T>) -> Unit)? = null
    var itemClickCallBack: ((position: Int, Item<T>) -> Unit)? = null
    fun setData(data: List<Item<T>>) {
        mData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MyViewHolder(
            ItemScriptGroupEditBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ScriptGroupEditAdapter<*>.MyViewHolder) {
            holder.onBindViewHolder(position)
        }
    }


    inner class MyViewHolder(private val binding: ItemScriptGroupEditBinding) :
        BaseViewHolder(binding.root) {
        override fun onBindViewHolder(position: Int) {
            val stringBuilder = StringBuilder()
            val item = mData[position]
            if (isShowPosition) {
                stringBuilder.append("编号:$position: \n")
            }
            stringBuilder.append("名称:${item.name} \n")
            stringBuilder.append("信息:${item.msg} \n")
            binding.tvName.text = stringBuilder.toString()
            binding.btnEdit.setOnClickListener {
                editCallBack?.invoke(position, item)
            }
            binding.btnDelete.setOnClickListener {
                deleteCallBack?.invoke(position, item)
            }
            binding.llItem.setOnClickListener {
                itemClickCallBack?.invoke(position, item)
            }
            if (btnVisibility) {
                binding.llBtn.visibility = View.VISIBLE
            } else {
                binding.llBtn.visibility = View.GONE
            }
        }
    }


}

