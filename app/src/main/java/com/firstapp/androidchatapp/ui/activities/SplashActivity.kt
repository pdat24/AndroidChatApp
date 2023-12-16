package com.firstapp.androidchatapp.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.firstapp.androidchatapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val image: ImageView = findViewById(R.id.ivAppImg)
        image.animate()
            .scaleX(1.25f)
            .scaleY(1.25f).duration = 1000

        lifecycleScope.launch {
            delay(2000)
            withContext(Dispatchers.Main) {
                startActivity(
                    Intent(this@SplashActivity, LoginActivity::class.java)
                )
                finish()
            }
        }
    }
}