package com.firstapp.androidchatapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.ImageAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var slide: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private val images = listOf(
        R.drawable.login_screen_thumbnail,
        R.drawable.login_screen_thumbnail_2,
        R.drawable.login_screen_thumbnail_3,
        R.drawable.login_screen_thumbnail_4
    )
    private val SLIDE_ITEM_SIZE = images.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // get views
        slide = findViewById(R.id.vp2Slide)
        indicatorContainer = findViewById(R.id.indicatorContainer)

        // create slide
        slide.adapter = ImageAdapter(images)
        slide.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                changeActiveIndicator(position)
            }
        })
        autoForwardSlideItem()

        // create indicators
        for (i in 0 until SLIDE_ITEM_SIZE) {
            LayoutInflater.from(this).inflate(R.layout.view_indicator, indicatorContainer, true)
        }
    }

    fun navigateToSignInScreen(view: View) {
        startActivity(
            Intent(this, SignUpActivity::class.java)
        )
    }

    private fun changeActiveIndicator(position: Int) {
        indicatorContainer.getChildAt(position)
            .findViewById<FrameLayout>(R.id.innerView)
            .setBackgroundColor(getColor(R.color.indicator))
        // unset background for others
        for (i in 0 until SLIDE_ITEM_SIZE) {
            if (i != position) {
                indicatorContainer.getChildAt(i)
                    .findViewById<FrameLayout>(R.id.innerView)
                    .background = null
            }
        }
    }

    private fun autoForwardSlideItem() {
        lifecycleScope.launch {
            delay(5000)
            if (slide.currentItem == SLIDE_ITEM_SIZE - 1) {
                slide.currentItem = 0
            } else slide.currentItem++
            autoForwardSlideItem()
        }
    }
}