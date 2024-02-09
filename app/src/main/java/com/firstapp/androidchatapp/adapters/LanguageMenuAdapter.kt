package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.utils.Constants
import com.firstapp.androidchatapp.utils.Constants.Companion.LANGUAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON

class LanguageMenuAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items) {

    private var sharedPreferences: SharedPreferences? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (sharedPreferences == null)
            sharedPreferences =
                context.getSharedPreferences(MAIN_SHARED_PREFERENCE, Context.MODE_PRIVATE)
        val item = items[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val tvLanguage = view.findViewById<TextView>(R.id.tvLanguage)
        tvLanguage.text = item
        val languageCode = sharedPreferences!!.getString(LANGUAGE, "en")

        view.findViewById<ImageView>(R.id.iconCheck).visibility = if (
            languageCode == "en" && item == context.getString(R.string.english) ||
            languageCode == "vi" && item == context.getString(R.string.vietnamese)
        ) View.VISIBLE else View.INVISIBLE

        tvLanguage.setTextColor(
            if (sharedPreferences!!.getBoolean(NIGHT_MODE_ON, false))
                Color.parseColor("#C3C1C1")
            else Color.parseColor("#605C5C")
        )
        return view
    }
}