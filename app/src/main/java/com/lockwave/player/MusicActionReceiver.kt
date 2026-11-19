
package com.lockwave.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MusicActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LOCKWAVE_ACTIONS"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val position = if (
            intent.action == MusicSessionManager.ACTION_SEEK
        ) {
            intent.getLongExtra(
                MusicSessionManager.EXTRA_POSITION,
                -1L
            )
        } else {
            null
        }

        val success = MusicListenerService.dispatchAction(
            intent.action,
            position
        )

        if (!success) {
            Log.w(
                TAG,
                "Could not dispatch playback action: ${intent.action}"
            )
        }
    }
}
