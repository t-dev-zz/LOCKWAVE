
package com.lockwave.player

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MusicListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LOCKWAVE"
        private const val REFRESH_DELAY = 350L

        @Volatile var currentTitle = "Nothing playing"
            private set

        @Volatile var currentArtist = ""
            private set

        @Volatile var currentAlbum = ""
            private set

        @Volatile var isPlaying = false
            private set

        @Volatile var currentDuration = 0L
            private set

        @Volatile var currentPosition = 0L
            private set

        @Volatile var playbackSpeed = 0f
            private set

        @Volatile var lastPositionUpdateTime = 0L
            private set

        @Volatile
        private var selectedController: MediaController? = null

        fun dispatchAction(
            action: String?,
            position: Long? = null
        ): Boolean {
            val controller = selectedController ?: return false

            return try {
                val controls = controller.transportControls

                when (action) {
                    MusicSessionManager.ACTION_PLAY ->
                        controls.play()

                    MusicSessionManager.ACTION_PAUSE ->
                        controls.pause()

                    MusicSessionManager.ACTION_NEXT ->
                        controls.skipToNext()

                    MusicSessionManager.ACTION_PREVIOUS ->
                        controls.skipToPrevious()

                    MusicSessionManager.ACTION_SEEK -> {
                        val target = position ?: return false
                        if (target < 0L) return false
                        controls.seekTo(target)
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

    private val handler = Handler(Looper.getMainLooper())

    private var activeController: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null

    private var sessionManager: MusicSessionManager? = null
    private var platformSessionManager: MediaSessionManager? = null

    private var sessionsListener:
        MediaSessionManager.OnActiveSessionsChangedListener? = null

    private var listenerRegistered = false

    private val refreshRunnable = Runnable {
        refreshSessions()
    }

    private fun scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, REFRESH_DELAY)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        Log.d(TAG, "LOCKWAVE 5.0 connected")

        sessionManager = MusicSessionManager(this)

        try {
            platformSessionManager = getSystemService(
                Context.MEDIA_SESSION_SERVICE
            ) as MediaSessionManager

            val component = ComponentName(
                this,
                MusicListenerService::class.java
            )

            sessionsListener =
                MediaSessionManager.OnActiveSessionsChangedListener {
                    scheduleRefresh()
                }

            platformSessionManager?.addOnActiveSessionsChangedListener(
                sessionsListener,
                component,
                handler
            )

            listenerRegistered = true

            refreshSessions()

        } catch (e: SecurityException) {
            Log.e(TAG, "Notification access is required", e)
        } catch (e: Exception) {
            Log.e(TAG, "Could not register session listener", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName == packageName) return
        scheduleRefresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName == packageName) return
        scheduleRefresh()
    }

    private fun refreshSessions() {
        val manager = platformSessionManager ?: return

        try {
            val component = ComponentName(
                this,
                MusicListenerService::class.java
            )

            val sessions = manager.getActiveSessions(component)
                .filter { it.packageName != packageName }

            val existingToken = activeController?.sessionToken

            val existing = sessions.firstOrNull {
                it.sessionToken == existingToken
            }

            val selected =
                sessions.firstOrNull {
                    it.playbackState?.state ==
                        PlaybackState.STATE_PLAYING
                } ?: existing
                ?: sessions.firstOrNull {
                    it.playbackState?.state !=
                        PlaybackState.STATE_NONE
                }
                ?: sessions.firstOrNull()

            if (selected == null) {
                clearCurrentTrack()
                return
            }

            val sameSession =
                activeController?.sessionToken ==
                    selected.sessionToken

            if (!sameSession) {
                unregisterController()

                activeController = selected
                selectedController = selected

                val callback = object : MediaController.Callback() {

                    override fun onMetadataChanged(
                        metadata: android.media.MediaMetadata?
                    ) {
                        if (activeController?.sessionToken ==
                            selected.sessionToken
                        ) {
                            updateTrack(selected)
                        }
                    }

                    override fun onPlaybackStateChanged(
                        state: PlaybackState?
                    ) {
                        if (activeController?.sessionToken ==
                            selected.sessionToken
                        ) {
                            updateTrack(selected)
                        }
                    }

                    override fun onSessionDestroyed() {
                        scheduleRefresh()
                    }
                }

                controllerCallback = callback
                selected.registerCallback(callback)
            }

            selectedController = selected
            updateTrack(selected)

        } catch (e: SecurityException) {
            Log.e(TAG, "Session access denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Session refresh failed", e)
        }
    }

    private fun updateTrack(controller: MediaController) {
        val metadata = controller.metadata
        val playback = controller.playbackState

        val title = metadata
            ?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown track"

        val artist = metadata
            ?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown artist"

        val album = metadata
            ?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""

        val duration = (
            metadata?.getLong(
                android.media.MediaMetadata.METADATA_KEY_DURATION
            ) ?: 0L
        ).coerceAtLeast(0L)

        val position = (
            playback?.position ?: 0L
        ).coerceAtLeast(0L)

        val speed = playback?.playbackSpeed ?: 0f

        val playing =
            playback?.state == PlaybackState.STATE_PLAYING

        currentTitle = title
        currentArtist = artist
        currentAlbum = album
        currentDuration = duration
        currentPosition = position
        playbackSpeed = speed
        isPlaying = playing

        lastPositionUpdateTime = SystemClock.elapsedRealtime()

        selectedController = controller

        sessionManager?.update(controller)
    }

    private fun clearCurrentTrack() {
        if (
            activeController == null &&
            currentTitle == "Nothing playing"
        ) {
            return
        }

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
            controllerCallback?.let { callback ->
                activeController?.unregisterCallback(callback)
            }
        } catch (_: Exception) {
        }

        controllerCallback = null
        activeController = null
        selectedController = null
    }

    private fun unregisterSessionListener() {
        if (!listenerRegistered) return

        try {
            sessionsListener?.let {
                platformSessionManager
                    ?.removeOnActiveSessionsChangedListener(it)
            }
        } catch (_: Exception) {
        }

        sessionsListener = null
        listenerRegistered = false
    }

    override fun onListenerDisconnected() {
        handler.removeCallbacksAndMessages(null)

        unregisterSessionListener()
        unregisterController()

        sessionManager?.release()
        sessionManager = null

        platformSessionManager = null

        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)

        unregisterSessionListener()
        unregisterController()

        sessionManager?.release()
        sessionManager = null

        super.onDestroy()
    }
}
