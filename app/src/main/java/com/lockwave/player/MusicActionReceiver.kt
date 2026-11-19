
package com.lockwave.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.content.ComponentName
import android.util.Log

class MusicActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LOCKWAVE_ACTIONS"
    }

    override fun onReceive(context: Context, intent: Intent) {

        try {
            val manager = context.getSystemService(
                Context.MEDIA_SESSION_SERVICE
            ) as MediaSessionManager

            val sessions = manager.getActiveSessions(
                ComponentName(
                    context,
                    MusicListenerService::class.java
                )
            )

            val controller = sessions.firstOrNull {
                it.playbackState?.state ==
                    PlaybackState.STATE_PLAYING
            } ?: sessions.firstOrNull()

            if (controller == null) {
                Log.w(TAG, "No active music session")
                return
            }

            when (intent.action) {

                MusicSessionManager.ACTION_PLAY ->
                    controller.transportControls.play()

                MusicSessionManager.ACTION_PAUSE ->
                    controller.transportControls.pause()

                MusicSessionManager.ACTION_NEXT ->
                    controller.transportControls.skipToNext()

                MusicSessionManager.ACTION_PREVIOUS ->
                    controller.transportControls.skipToPrevious()

                else ->
                    Log.w(TAG, "Unknown action: ${intent.action}")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Notification access permission missing", e)
        } catch (e: Exception) {
            Log.e(TAG, "Playback action failed", e)
        }
    }
}
