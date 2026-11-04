package com.weave.notebook

import android.app.Application

/**
 * Application entry. Offline handwriting notebook — no network, analytics, or cloud.
 */
class WeaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this)
        com.weave.notebook.data.NotebookIO.database = com.weave.notebook.data.NotebookDatabase(this)
    }
}
