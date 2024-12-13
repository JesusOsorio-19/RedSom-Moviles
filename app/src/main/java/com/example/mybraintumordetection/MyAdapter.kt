package com.example.mybraintumordetection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.mybraintumordetection.R

class MyAdapter(private val context: Context, private val arraylist: java.util.ArrayList<BrainTumor>) : BaseAdapter() {

    private lateinit var txtminombre: TextView
    private lateinit var txtmibraintumor: TextView
    private lateinit var imgimagen: ImageView
    override fun getCount(): Int {
        return arraylist.size
    }
    override fun getItem(position: Int): Any {
        return position
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        convertView = LayoutInflater.from(context).inflate(R.layout.misfilas, parent, false)
        txtminombre = convertView.findViewById(R.id.txtminombre)
        txtmibraintumor = convertView.findViewById(R.id.txtmibraintumor)
        imgimagen  = convertView.findViewById(R.id.imgimagen)

        txtminombre.text = arraylist[position].nombre
        txtmibraintumor.text = arraylist[position].existeBrainTumor

        Glide.with(context)
            .load(arraylist[position].urlBrainTumor)
            .into(imgimagen);
        return convertView
    }
}