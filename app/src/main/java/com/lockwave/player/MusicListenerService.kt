
package com.lockwave.player

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MusicListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LOCKWAVE"

        var currentTitle = "Nothing playing"
            private set

        var currentArtist = ""
            private set

        var currentAlbum = ""
            private set

        var isPlaying = false
            private set
    }

    private var activeController: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null
    private var sessionManager: MusicSessionManager? = null

    override fun onListenerConnected() {
        super.onListenerConnected()

        Log.d(TAG, "LOCKWAVE music engine connected")

        sessionManager = MusicSessionManager(this)

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

            // Exclude LOCKWAVE's own media session.
            val externalSessions = sessions.filter {
                it.packageName != packageName
            }

            val selected = externalSessions.firstOrNull {
                it.playbackState?.state ==
                    PlaybackState.STATE_PLAYING
            } ?: externalSessions.firstOrNull()

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
                            metadata: android.media.MediaMetadata?
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
            Log.e(TAG, "Notification access required", e)
        } catch (e: Exception) {
            Log.e(TAG, "Session detection failed", e)
        }
    }

    private fun updateTrack(controller: MediaController) {

        val metadata = controller.metadata
        val playback = controller.playbackState

        currentTitle = metadata?.getString(
            android.media.MediaMetadata.METADATA_KEY_TITLE
        ) ?: "Unknown track"

        currentArtist = metadata?.getString(
            android.media.MediaMetadata.METADATA_KEY_ARTIST
        ) ?: "Unknown artist"

        currentAlbum = metadata?.getString(
            android.media.MediaMetadata.METADATA_KEY_ALBUM
        ) ?: ""

        isPlaying = playback?.state ==
            PlaybackState.STATE_PLAYING

        sessionManager?.update(controller)

        Log.d(
            TAG,
            "Now playing: $currentTitle - $currentArtist"
        )
    }

    private fun clearCurrentTrack() {

        unregisterController()

        currentTitle = "Nothing playing"
        currentArtist = ""
        currentAlbum = ""
        isPlaying = false

        sessionManager?.update(null)
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
        sessionManager?.release()
        sessionManager = null

        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        unregisterController()
        sessionManager?.release()
        sessionManager = null

        super.onDestroy()
    }
}
