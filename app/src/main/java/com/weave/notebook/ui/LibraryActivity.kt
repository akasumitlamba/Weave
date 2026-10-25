package com.weave.notebook.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.BuildConfig
import com.weave.notebook.R
import com.weave.notebook.databinding.ActivityLibraryBinding
import com.weave.notebook.input.InputProbeActivity

/**
 * Notes library placeholder. Shows brand + tagline here (not on the writing toolbar).
 * Real create/open/rename/delete arrives in PR 14.
 */
class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.brandName.text = getString(R.string.app_name)
        binding.tagline.text = getString(R.string.tagline)
        binding.libraryStatus.text = getString(R.string.library_empty_hint)

        binding.newNoteButton.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java).apply {
                putExtra(EditorActivity.EXTRA_NOTE_TITLE, getString(R.string.default_note_title))
            })
        }

        if (BuildConfig.DEBUG) {
            binding.openProbeButton.visibility = android.view.View.VISIBLE
            binding.openProbeButton.setOnClickListener {
                startActivity(Intent(this, InputProbeActivity::class.java))
            }
        } else {
            binding.openProbeButton.visibility = android.view.View.GONE
        }
    }
}
