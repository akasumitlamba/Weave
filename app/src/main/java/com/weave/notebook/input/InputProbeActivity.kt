package com.weave.notebook.input

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.weave.notebook.BuildConfig
import com.weave.notebook.R
import com.weave.notebook.databinding.ActivityInputProbeBinding


/**
 * Debug-only screen for capturing tool type, pointer history, size, pressure, CANCEL.
 * Not retained across process death beyond the in-memory buffer while open.
 */
class InputProbeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputProbeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            finish()
            return
        }
        binding = ActivityInputProbeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.probeTitle.text = getString(R.string.input_probe_title)
        binding.clearButton.setOnClickListener {
            binding.probeView.recorder.clear()
            updateLog(emptyList())
            updateReport()
            binding.probeView.invalidate()
        }
        binding.reportButton.setOnClickListener { updateReport() }
        binding.probeView.onSamplesChanged = { samples ->
            updateLog(samples.takeLast(40))
        }
        updateReport()
    }

    private fun updateLog(samples: List<InputSample>) {
        binding.sampleLog.text = samples.joinToString("\n") { s ->
            buildString {
                append("${s.eventAction} id=${s.pointerId} ${s.toolTypeLabel} ")
                append("x=${"%.1f".format(s.x)} y=${"%.1f".format(s.y)} ")
                append("sz=${"%.3f".format(s.size)} p=${"%.3f".format(s.pressure)}")
                if (s.canceled) append(" CANCEL")
                if (s.isHistorical) append(" hist")
            }
        }
        binding.sampleLog.post {
            (binding.sampleLog.parent as? ScrollView)?.fullScroll(TextView.FOCUS_DOWN)
        }
    }

    private fun updateReport() {
        val report = binding.probeView.recorder.buildReport(deviceVerified = false)
        binding.reportText.text = getString(
            R.string.input_probe_report_template,
            report.stylusIdentity.name,
            report.sawFinger.toString(),
            report.sawStylus.toString(),
            report.sampleCount.toString(),
            report.notes
        )
    }

    companion object {
        const val CAPTURE_STEPS = """
Weave input probe — capture steps (TCL TAB 10s)
1. Install a debug build and open Input Probe from the bootstrap screen.
2. With the passive stylus, draw a short stroke. Note tool type in the log.
3. With a finger, draw a short stroke. Compare tool type / size / pressure.
4. Two-finger contact: confirm POINTER_DOWN / second id appears.
5. Trigger CANCEL if possible (system gesture / incoming interruption) and confirm CANCEL samples.
6. Tap Report. Record stylusIdentity finding. If both contacts are FINGER, stylus identity is absent.
7. Do not screenshot personal handwriting for retention; copy the summary fields only.
"""
    }
}
