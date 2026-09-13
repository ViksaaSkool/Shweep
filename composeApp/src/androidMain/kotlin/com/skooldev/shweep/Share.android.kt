package com.skooldev.shweep

import android.content.Intent
import android.widget.Toast

actual fun shareText(text: String, title: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ShweepApplication.instance.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(ShweepApplication.instance, "No app available to share", Toast.LENGTH_SHORT).show()
    }
}
