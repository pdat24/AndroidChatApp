package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.firstapp.androidchatapp.R

class LanguageMenuAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<String>,
    private val activeLanguageCode: String
) : ArrayAdapter<String>(context, resource, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        view.findViewById<TextView>(R.id.tvLanguage).text = item
        view.findViewById<ImageView>(R.id.iconCheck).visibility = if (
            activeLanguageCode == "en" && item == context.getString(R.string.english) ||
            activeLanguageCode == "vi" && item == context.getString(R.string.vietnamese)
        ) View.VISIBLE else View.INVISIBLE
        return view
    }
}