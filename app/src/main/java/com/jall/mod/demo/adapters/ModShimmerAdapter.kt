package com.jall.mod.demo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jall.mod.demo.R

class ModShimmerAdapter(private val itemCount: Int) : RecyclerView.Adapter<ModShimmerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mod_shimmer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

    }

    override fun getItemCount(): Int = itemCount

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}