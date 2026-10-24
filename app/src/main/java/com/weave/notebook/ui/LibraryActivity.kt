package com.weave.notebook.ui

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.BuildConfig
import com.weave.notebook.R
import com.weave.notebook.data.NoteMeta
import com.weave.notebook.data.WeaveRepository
import com.weave.notebook.databinding.ActivityLibraryBinding
import com.weave.notebook.input.InputProbeActivity
import com.weave.notebook.model.PaperType

/**
 * Notes library: create, open, rename, duplicate, delete with confirmation.
 * Tagline lives here — not on the writing toolbar.
 */
class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private val store get() = WeaveRepository.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.brandName.text = getString(R.string.app_name)
        binding.tagline.text = getString(R.string.tagline)

        binding.newNoteButton.setOnClickListener { showNewNoteDialog() }

        if (BuildConfig.DEBUG) {
            binding.openProbeButton.visibility = android.view.View.VISIBLE
            binding.openProbeButton.setOnClickListener {
                startActivity(Intent(this, InputProbeActivity::class.java))
            }
        } else {
            binding.openProbeButton.visibility = android.view.View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val notes = store.listNotes()
        binding.notesContainer.removeAllViews()
        if (notes.isEmpty()) {
            binding.libraryStatus.visibility = android.view.View.VISIBLE
            binding.libraryStatus.text = getString(R.string.library_empty_hint)
            return
        }
        binding.libraryStatus.visibility = android.view.View.GONE
        val inflater = LayoutInflater.from(this)
        for (note in notes) {
            val row = inflater.inflate(R.layout.item_note, binding.notesContainer, false)
            row.findViewById<TextView>(R.id.noteTitle).text = note.title
            row.findViewById<TextView>(R.id.noteMeta).text = getString(
                R.string.note_meta_format,
                note.pageIds.size,
                DateUtils.getRelativeTimeSpanString(note.modifiedAtMs)
            )
            row.setOnClickListener { openNote(note) }
            row.findViewById<TextView>(R.id.noteMenu).setOnClickListener { showNoteMenu(note) }
            binding.notesContainer.addView(row)
        }
    }

    private fun showNewNoteDialog() {
        val name = EditText(this).apply {
            hint = getString(R.string.note_name_hint)
            setText(R.string.default_note_title)
            minHeight = resources.getDimensionPixelSize(R.dimen.min_touch_target)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.new_note)
            .setView(name)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.create) { _, _ ->
                val title = name.text?.toString()?.ifBlank { getString(R.string.default_note_title) }
                    ?: getString(R.string.default_note_title)
                val note = store.createNote(title, PaperType.RULED)
                openNote(note)
            }
            .show()
    }

    private fun showNoteMenu(note: NoteMeta) {
        val items = arrayOf(
            getString(R.string.rename),
            getString(R.string.duplicate),
            getString(R.string.delete)
        )
        AlertDialog.Builder(this)
            .setTitle(note.title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> renameNote(note)
                    1 -> {
                        store.duplicate(note.noteId)
                        refreshList()
                    }
                    2 -> confirmDelete(note)
                }
            }
            .show()
    }

    private fun renameNote(note: NoteMeta) {
        val name = EditText(this).apply {
            setText(note.title)
            minHeight = resources.getDimensionPixelSize(R.dimen.min_touch_target)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(name)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.rename) { _, _ ->
                store.rename(note.noteId, name.text?.toString().orEmpty().ifBlank { note.title })
                refreshList()
            }
            .show()
    }

    private fun confirmDelete(note: NoteMeta) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_confirm, note.title))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                store.delete(note.noteId)
                refreshList()
            }
            .show()
    }

    private fun openNote(note: NoteMeta) {
        startActivity(Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_NOTE_TITLE, note.title)
            putExtra(EditorActivity.EXTRA_NOTE_ID, note.noteId)
        })
    }
}
