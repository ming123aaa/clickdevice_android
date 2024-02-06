package com.example.clickdevice.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.text.FieldPosition

abstract class BaseViewHolder(item:View):RecyclerView.ViewHolder(item) {

    abstract fun onBindViewHolder(position: Int)
}