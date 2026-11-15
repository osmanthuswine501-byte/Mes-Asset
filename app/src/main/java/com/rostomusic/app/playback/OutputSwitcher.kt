package com.rostomusic.app.playback

import android.content.Context
import android.content.Intent
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import androidx.mediarouter.app.SystemOutputSwitcherDialogController

/**
 * Opens Android's output picker, where the user moves the sound between the phone speaker,
 * wired or Bluetooth headphones, Bluetooth speakers and the car.
 */
object OutputSwitcher {
    fun open(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            val shown = runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)
            if (shown) return
        }
        val shown = runCatching { SystemOutputSwitcherDialogController.showDialog(context) }.getOrDefault(false)
        if (shown) return
        // Android 10 and older have no output picker: the Bluetooth settings are the closest place.
        runCatching {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
