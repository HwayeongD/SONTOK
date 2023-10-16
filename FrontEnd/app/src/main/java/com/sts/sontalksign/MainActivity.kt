package com.sts.sontalksign

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sts.sontalksign.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
    }
}