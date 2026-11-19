
package com.lockwave.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
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

        var currentDuration = 0L
            private set

        var currentPosition = 0L
            private set

        var playbackSpeed = 0f
            private set

        var lastPositionUpdateTime = 0L
            private set

        private var selectedController: MediaController? = null

        fun dispatchAction(
            action: String?,
            position: Long? = null
        ): Boolean {
            val controller = selectedController ?: return false

            return try {
                when (action) {
                    MusicSessionManager.ACTION_PLAY ->
                        controller.transportControls.play()

                    MusicSessionManager.ACTION_PAUSE ->
                        controller.transportControls.pause()

                    MusicSessionManager.ACTION_NEXT ->
                        controller.transportControls.skipToNext()

                    MusicSessionManager.ACTION_PREVIOUS ->
                        controller.transportControls.skipToPrevious()

                    MusicSessionManager.ACTION_SEEK -> {
                        val target = position ?: return false
                        if (target < 0L) return false
                        controller.transportControls.seekTo(target)
                    }

                    else -> return false
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Playback command failed", e)
                false
            }
        }
    }

    private var activeController: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null
    private var sessionManager: MusicSessionManager? = null

    override fun onListenerConnected() {
        super.onListenerConnected()

        Log.d(TAG, "LOCKWAVE 4.0 engine connected")

        sessionManager = MusicSessionManager(this)
        refreshSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    private fun refreshSessions() {
        try {
            val manager = getSystemService(
                Context.MEDIA_SESSION_SERVICE
            ) as android.media.session.MediaSessionManager

            val sessions = manager.getActiveSessions(
                ComponentName(
                    this,
                    MusicListenerService::class.java
                )
            )

            val externalSessions = sessions.filter {
                it.packageName != packageName
            }

            val existing = activeController?.let { current ->
                externalSessions.firstOrNull {
                    it.sessionToken == current.sessionToken
                }
            }

            val selected =
                externalSessions.firstOrNull {
                    it.playbackState?.state ==
                        PlaybackState.STATE_PLAYING
                } ?: existing ?: externalSessions.firstOrNull {
                    it.playbackState?.state != PlaybackState.STATE_NONE
                } ?: externalSessions.firstOrNull()

            if (selected == null) {
                clearCurrentTrack()
                return
            }

            if (
                activeController?.sessionToken !=
                selected.sessionToken
            ) {
                unregisterController()

                activeController = selected
                selectedController = selected

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

                selected.registerCallback(controllerCallback!!)
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
            MediaMetadata.METADATA_KEY_TITLE
        )?.takeIf {
            it.isNotBlank()
        } ?: "Unknown track"

        currentArtist = metadata?.getString(
            MediaMetadata.METADATA_KEY_ARTIST
        )?.takeIf {
            it.isNotBlank()
        } ?: "Unknown artist"

        currentAlbum = metadata?.getString(
            MediaMetadata.METADATA_KEY_ALBUM
        ) ?: ""

        currentDuration = (
            metadata?.getLong(
                MediaMetadata.METADATA_KEY_DURATION
            ) ?: 0L
        ).coerceAtLeast(0L)

        currentPosition = (
            playback?.position ?: 0L
        ).coerceAtLeast(0L)

        playbackSpeed = playback?.playbackSpeed ?: 0f

        isPlaying =
            playback?.state == PlaybackState.STATE_PLAYING

        lastPositionUpdateTime =
            SystemClock.elapsedRealtime()

        selectedController = controller
        sessionManager?.update(controller)

        Log.d(
            TAG,
            "Track: $currentTitle | " +
                "Playing: $isPlaying | " +
                "Position: $currentPosition | " +
                "Duration: $currentDuration"
        )
    }

    private fun clearCurrentTrack() {

        unregisterController()

        currentTitle = "Nothing playing"
        currentArtist = ""
        currentAlbum = ""

        isPlaying = false
        currentDuration = 0L
        currentPosition = 0L
        playbackSpeed = 0f
        lastPositionUpdateTime = 0L

        selectedController = null
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
        selectedController = null
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
