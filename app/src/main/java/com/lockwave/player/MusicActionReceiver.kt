
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
        val action = intent.action ?: return

        val position = if (
            action == MusicSessionManager.ACTION_SEEK
        ) {
            intent.getLongExtra(
                MusicSessionManager.EXTRA_POSITION,
                -1L
            ).takeIf { it >= 0L }
        } else {
            null
        }

        val validAction = action in setOf(
            MusicSessionManager.ACTION_PLAY,
            MusicSessionManager.ACTION_PAUSE,
            MusicSessionManager.ACTION_NEXT,
            MusicSessionManager.ACTION_PREVIOUS,
            MusicSessionManager.ACTION_SEEK
        )

        if (!validAction) return

        val success = MusicListenerService.dispatchAction(
            action,
            position
        )

        if (!success) {
            Log.w(TAG, "Playback action unavailable: $action")
        }
    }
}
