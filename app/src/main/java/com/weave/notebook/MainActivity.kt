package com.weave.notebook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.databinding.ActivityMainBinding

/**
 * Bootstrap shell. Library and editor screens land in later milestones.
 * Tagline "Connecting ideas together" belongs on the library screen (PR 14),
 * not the writing toolbar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appName.text = getString(R.string.app_name)
        binding.bootstrapStatus.text = getString(R.string.bootstrap_status)
    }
}
