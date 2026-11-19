
package com.lockwave.player

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MusicListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LOCKWAVE"

        var currentTitle: String = "Nothing playing"
            private set

        var currentArtist: String = ""
            private set

        var currentAlbum: String = ""
            private set

        var isPlaying: Boolean = false
            private set
    }

    private var activeController: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Music listener connected")
        refreshSessions()
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification?
    ) {
        refreshSessions()
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?
    ) {
        refreshSessions()
    }

    private fun refreshSessions() {
        try {
            val manager = getSystemService(
                MEDIA_SESSION_SERVICE
            ) as android.media.session.MediaSessionManager

            val sessions = manager.getActiveSessions(
                ComponentName(
                    this,
                    MusicListenerService::class.java
                )
            )

            val selected = sessions.firstOrNull {
                it.playbackState?.state ==
                    PlaybackState.STATE_PLAYING
            } ?: sessions.firstOrNull()

            if (selected == null) {
                clearCurrentTrack()
                return
            }

            if (activeController?.sessionToken !=
                selected.sessionToken
            ) {
                unregisterController()
                activeController = selected

                controllerCallback =
                    object : MediaController.Callback() {

                        override fun onMetadataChanged(
                            metadata: MediaMetadata?
                        ) {
                            updateTrack(selected)
                        }

                        override fun onPlaybackStateChanged(
                            state: PlaybackState?
                        ) {
                            updateTrack(selected)
                        }

                        override fun onSessionDestroyed() {
                            refreshSessions()
                        }
                    }

                selected.registerCallback(
                    controllerCallback!!
                )
            }

            updateTrack(selected)

        } catch (e: SecurityException) {
            Log.e(TAG, "Notification access is not enabled", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read music session", e)
        }
    }

    private fun updateTrack(controller: MediaController) {
        val metadata = controller.metadata
        val state = controller.playbackState

        currentTitle = metadata?.getString(
            MediaMetadata.METADATA_KEY_TITLE
        ) ?: "Unknown track"

        currentArtist = metadata?.getString(
            MediaMetadata.METADATA_KEY_ARTIST
        ) ?: "Unknown artist"

        currentAlbum = metadata?.getString(
            MediaMetadata.METADATA_KEY_ALBUM
        ) ?: ""

        isPlaying = state?.state ==
            PlaybackState.STATE_PLAYING

        Log.d(
            TAG,
            "Track: $currentTitle - $currentArtist | Playing: $isPlaying"
        )
    }

    private fun clearCurrentTrack() {
        unregisterController()

        currentTitle = "Nothing playing"
        currentArtist = ""
        currentAlbum = ""
        isPlaying = false
    }

    private fun unregisterController() {
        try {
            controllerCallback?.let {
                activeController?.unregisterCallback(it)
            }
        } catch (_: Exception) {
        }

        controllerCallback = null
        activeController = null
    }

    override fun onListenerDisconnected() {
        unregisterController()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        unregisterController()
        super.onDestroy()
    }
}
