
package com.lockwave.player

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
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

            val externalSessions = sessions.filter {
                it.packageName != context.packageName
            }

            val controller = externalSessions.firstOrNull {
                it.playbackState?.state ==
                    PlaybackState.STATE_PLAYING
            } ?: externalSessions.firstOrNull {
                it.playbackState?.state != PlaybackState.STATE_NONE
            }

            if (controller == null) {
                Log.w(TAG, "No external music session found")
                return
            }

            when (intent.action) {

                MusicSessionManager.ACTION_PLAY -> {
                    controller.transportControls.play()
                }

                MusicSessionManager.ACTION_PAUSE -> {
                    controller.transportControls.pause()
                }

                MusicSessionManager.ACTION_NEXT -> {
                    controller.transportControls.skipToNext()
                }

                MusicSessionManager.ACTION_PREVIOUS -> {
                    controller.transportControls.skipToPrevious()
                }

                MusicSessionManager.ACTION_SEEK -> {
                    val position = intent.getLongExtra(
                        MusicSessionManager.EXTRA_POSITION,
                        -1L
                    )

                    if (position >= 0L) {
                        controller.transportControls.seekTo(position)
                    } else {
                        Log.w(TAG, "Invalid seek position")
                    }
                }

                else -> {
                    Log.w(TAG, "Unknown action: ${intent.action}")
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Notification access permission missing", e)
        } catch (e: Exception) {
            Log.e(TAG, "Playback action failed", e)
        }
    }
}
