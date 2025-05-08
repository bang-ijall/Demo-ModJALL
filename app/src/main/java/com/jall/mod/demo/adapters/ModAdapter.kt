package com.jall.mod.demo.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.jall.mod.demo.R
import com.jall.mod.demo.datas.ModData
import com.jall.mod.demo.preferences.MenuPreference
import com.jall.mod.demo.services.FloatingService
import com.jall.mod.demo.views.LoadingDialog

class ModAdapter : RecyclerView.Adapter<ModAdapter.ViewHolder>() {
    private val list = ArrayList<ModData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_mod, parent, false), parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setList(data: List<ModData>) {
        list.clear()
        list.addAll(data)
    }

    fun clear() {
        list.clear()
    }

    inner class ViewHolder(private val view: View, private val context: Context) :
        RecyclerView.ViewHolder(view.findViewById(R.id.main)) {
        fun bind(data: ModData) {
            val ivIcon = view.findViewById<ImageView>(R.id.iv_icon)
            val tvPackage = view.findViewById<MaterialTextView>(R.id.tv_package)
            val tvName = view.findViewById<MaterialTextView>(R.id.tv_name)
            val tvVersion =  view.findViewById<MaterialTextView>(R.id.tv_version)
            val btnLaunch = view.findViewById<MaterialButton>(R.id.btn_launch)
            Glide.with(context)
                .load(data.icon)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop().into(ivIcon)
            tvPackage.text = data.packageName
            tvName.text = data.name
            tvVersion.text = data.version

            btnLaunch.setOnClickListener {
                val progress = LoadingDialog(context)
                progress.show()

                if (FloatingService.isStarted) {
                    context.stopService(Intent(context, FloatingService::class.java))
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    MenuPreference.init(context, data.packageName)
                    val intent = context.packageManager.getLaunchIntentForPackage(data.packageName)
                    progress.dismiss()

                    if (intent != null) {
                        context.startService(Intent(context, FloatingService::class.java).apply {
                            putExtra(FloatingService.EXTRA_NAME, data.name)
                        })
                        context.startActivity(intent)
                    } else {
                        Snackbar.make(view, "${data.name} not installed", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }, 1000)
            }
        }
    }
}