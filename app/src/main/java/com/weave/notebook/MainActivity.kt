package com.weave.notebook

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.databinding.ActivityMainBinding
import com.weave.notebook.input.InputProbeActivity
import com.weave.notebook.model.PageSpec
import com.weave.notebook.model.PaperType
import java.util.UUID

/**
 * Bootstrap shell with A4 page preview. Library/editor arrive in later milestones.
 * Tagline belongs on the library screen (PR 14), not the writing toolbar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.appName.text = getString(R.string.app_name)
        binding.bootstrapStatus.text = getString(R.string.bootstrap_status)

        binding.pagePreview.page = PageSpec(
            pageId = UUID.randomUUID().toString(),
            paperType = PaperType.RULED,
            ruleSpacingMm = 8f
        )

        if (BuildConfig.DEBUG) {
            binding.openProbeButton.visibility = View.VISIBLE
            binding.openProbeButton.setOnClickListener {
                startActivity(Intent(this, InputProbeActivity::class.java))
            }
        } else {
            binding.openProbeButton.visibility = View.GONE
        }
    }
}
