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
import com.firstapp.androidchatapp.adapters.SlideItemAdapter
import com.firstapp.androidchatapp.models.SlideItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IntroduceActivity : AppCompatActivity() {

    private lateinit var slide: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var slideItems: List<SlideItem>
    private var slideItemSize: Int = 0
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_introduce)

        // get views
        slide = findViewById(R.id.vp2Slide)
        indicatorContainer = findViewById(R.id.indicatorContainer)

        // create slide items
        slideItems = listOf(
            SlideItem(
                R.drawable.login_screen_thumbnail,
                getString(R.string.introduce_title_1),
                getString(R.string.introduce_desc)
            ),
            SlideItem(
                R.drawable.login_screen_thumbnail_2,
                getString(R.string.introduce_title_2),
                getString(R.string.introduce_desc)
            ),
            SlideItem(
                R.drawable.login_screen_thumbnail_3,
                getString(R.string.introduce_title_3),
                getString(R.string.introduce_desc)
            ),
            SlideItem(
                R.drawable.login_screen_thumbnail_4,
                getString(R.string.introduce_title_4),
                getString(R.string.introduce_desc)
            )
        )
        slideItemSize = slideItems.size

        // create slide
        slide.adapter = SlideItemAdapter(slideItems)
        slide.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                changeActiveIndicator(position)
            }
        })
        // auto forward slide items
        autoForwardSlideItem()

        // create indicators
        for (i in 0 until slideItemSize) {
            LayoutInflater.from(this).inflate(R.layout.view_indicator, indicatorContainer, true)
        }
    }

    override fun onStart() {
        super.onStart()
        tryAutoLogin()
    }

    private fun tryAutoLogin() {
        if (firebaseAuth.currentUser != null) {
            println(firebaseAuth.currentUser?.displayName)
            // user signed in
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun navigateToSignInScreen(view: View) {
        startActivity(
            Intent(this, SignInActivity::class.java)
        )
    }

    private fun changeActiveIndicator(position: Int) {
        indicatorContainer.getChildAt(position)
            .findViewById<FrameLayout>(R.id.innerView)
            .background = null
        // unset background for others
        for (i in 0 until slideItemSize) {
            if (i != position) {
                indicatorContainer.getChildAt(i)
                    .findViewById<FrameLayout>(R.id.innerView)
                    .setBackgroundColor(getColor(R.color.indicator))
            }
        }
    }

    private fun autoForwardSlideItem() {
        lifecycleScope.launch {
            delay(5000)
            if (slide.currentItem == slideItemSize - 1) {
                slide.currentItem = 0
            } else slide.currentItem++
            autoForwardSlideItem()
        }
    }
}