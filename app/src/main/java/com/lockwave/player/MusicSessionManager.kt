
package com.lockwave.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock

class MusicSessionManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "lockwave_music"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.lockwave.player.PLAY"
        const val ACTION_PAUSE = "com.lockwave.player.PAUSE"
        const val ACTION_NEXT = "com.lockwave.player.NEXT"
        const val ACTION_PREVIOUS = "com.lockwave.player.PREVIOUS"
        const val ACTION_SEEK = "com.lockwave.player.SEEK"

        const val EXTRA_POSITION = "com.lockwave.player.POSITION"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    private val session = MediaSession(context, "LOCKWAVE")

    private var sourceController: MediaController? = null

    private var lastMetadataKey: String? = null
    private var lastPlaybackKey: String? = null
    private var lastNotificationKey: String? = null

    private var released = false

    init {
        createNotificationChannel()

        session.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        session.setSessionActivity(appIntent())

        session.setCallback(object : MediaSession.Callback() {

            override fun onPlay() {
                forward { it.transportControls.play() }
            }

            override fun onPause() {
                forward { it.transportControls.pause() }
            }

            override fun onSkipToNext() {
                forward { it.transportControls.skipToNext() }
            }

            override fun onSkipToPrevious() {
                forward { it.transportControls.skipToPrevious() }
            }

            override fun onSeekTo(position: Long) {
                if (position >= 0L) {
                    forward { it.transportControls.seekTo(position) }
                }
            }

            override fun onStop() {
                forward { it.transportControls.pause() }
            }
        })
    }

    private inline fun forward(
        command: (MediaController) -> Unit
    ) {
        val controller = sourceController ?: return

        try {
            command(controller)
        } catch (_: Exception) {
        }
    }

    fun update(controller: MediaController?) {
        if (released) return

        sourceController = controller

        if (controller == null) {
            lastMetadataKey = null
            lastPlaybackKey = null
            lastNotificationKey = null

            session.isActive = false
            session.setMetadata(null)

            session.setPlaybackState(
                PlaybackState.Builder()
                    .setState(
                        PlaybackState.STATE_NONE,
                        0L,
                        0f
                    )
                    .build()
            )

            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val metadata = controller.metadata
        val playback = controller.playbackState

        val title = metadata?.getString(
            MediaMetadata.METADATA_KEY_TITLE
        )?.takeIf { it.isNotBlank() } ?: "Unknown track"

        val artist = metadata?.getString(
            MediaMetadata.METADATA_KEY_ARTIST
        )?.takeIf { it.isNotBlank() } ?: "Unknown artist"

        val album = metadata?.getString(
            MediaMetadata.METADATA_KEY_ALBUM
        ) ?: ""

        val duration = (
            metadata?.getLong(
                MediaMetadata.METADATA_KEY_DURATION
            ) ?: 0L
        ).coerceAtLeast(0L)

        val metadataKey = "$title|$artist|$album|$duration"

        if (metadataKey != lastMetadataKey) {
            val newMetadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .build()

            session.setMetadata(newMetadata)
            lastMetadataKey = metadataKey
        }

        val state = playback?.state ?: PlaybackState.STATE_NONE
        val position = (playback?.position ?: 0L).coerceAtLeast(0L)
        val speed = playback?.playbackSpeed ?: 0f
        val sourceActions = playback?.actions ?: 0L

        var actions =
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE

        if (
            sourceActions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        ) {
            actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        }

        if (
            sourceActions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
        ) {
            actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        }

        if (
            sourceActions and PlaybackState.ACTION_SEEK_TO != 0L
        ) {
            actions = actions or PlaybackState.ACTION_SEEK_TO
        }

        val playbackKey =
            "$state|$position|$speed|$actions"

        if (playbackKey != lastPlaybackKey) {
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(actions)
                    .setState(
                        state,
                        position,
                        speed,
                        SystemClock.elapsedRealtime()
                    )
                    .build()
            )

            lastPlaybackKey = playbackKey
        }

        session.isActive = true

        val notificationKey = "$title|$artist|$state"

        if (notificationKey != lastNotificationKey) {
            showNotification(title, artist, state)
            lastNotificationKey = notificationKey
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LOCKWAVE Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Compact music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(
            context,
            MusicActionReceiver::class.java
        ).setAction(action)

        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun appIntent(): PendingIntent {
        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showNotification(
        title: String,
        artist: String,
        state: Int
    ) {
        val playing = state == PlaybackState.STATE_PLAYING

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(appIntent())
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(playing)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    actionIntent(ACTION_PREVIOUS)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    if (playing) {
                        android.R.drawable.ic_media_pause
                    } else {
                        android.R.drawable.ic_media_play
                    },
                    if (playing) "Pause" else "Play",
                    actionIntent(
                        if (playing) ACTION_PAUSE else ACTION_PLAY
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    actionIntent(ACTION_NEXT)
                ).build()
            )
            .build()

        try {
            notificationManager.notify(
                NOTIFICATION_ID,
                notification
            )
        } catch (_: SecurityException) {
            // Notification permission may be disabled by the user.
        }
    }

    fun release() {
        if (released) return
        released = true

        sourceController = null

        try {
            session.isActive = false
            session.release()
        } catch (_: Exception) {
        }

        notificationManager.cancel(NOTIFICATION_ID)
    }
}
