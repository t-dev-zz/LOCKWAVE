
package com.lockwave.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

    init {
        createNotificationChannel()

        session.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        session.setSessionActivity(appIntent())

        session.setCallback(object : MediaSession.Callback() {

            override fun onPlay() {
                sourceController?.transportControls?.play()
            }

            override fun onPause() {
                sourceController?.transportControls?.pause()
            }

            override fun onPlayFromMediaId(
                mediaId: String?,
                extras: android.os.Bundle?
            ) {
                sourceController?.transportControls?.play()
            }

            override fun onSkipToNext() {
                sourceController?.transportControls?.skipToNext()
            }

            override fun onSkipToPrevious() {
                sourceController?.transportControls?.skipToPrevious()
            }

            override fun onSeekTo(position: Long) {
                sourceController?.transportControls?.seekTo(position)
            }

            override fun onStop() {
                sourceController?.transportControls?.pause()
            }
        })
    }

    fun update(controller: MediaController?) {

        sourceController = controller

        if (controller == null) {
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
        ) ?: "Unknown track"

        val artist = metadata?.getString(
            MediaMetadata.METADATA_KEY_ARTIST
        ) ?: "Unknown artist"

        val artwork: Bitmap? =
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(
                    MediaMetadata.METADATA_KEY_ALBUM_ART
                )

        val duration = (
            metadata?.getLong(
                MediaMetadata.METADATA_KEY_DURATION
            ) ?: 0L
        ).coerceAtLeast(0L)

        val sessionMetadata = MediaMetadata.Builder()
            .putString(
                MediaMetadata.METADATA_KEY_TITLE,
                title
            )
            .putString(
                MediaMetadata.METADATA_KEY_ARTIST,
                artist
            )
            .putLong(
                MediaMetadata.METADATA_KEY_DURATION,
                duration
            )

        // Artwork is notification/session metadata only.
        // LOCKWAVE does not display an album-art screen.
        if (artwork != null) {
            sessionMetadata.putBitmap(
                MediaMetadata.METADATA_KEY_ART,
                artwork
            )
        }

        session.setMetadata(sessionMetadata.build())

        val state = playback?.state ?: PlaybackState.STATE_NONE
        val position = playback?.position ?: 0L
        val speed = playback?.playbackSpeed ?: 0f
        val sourceActions = playback?.actions ?: 0L

        var actions =
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE

        if (
            (sourceActions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L
        ) {
            actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        }

        if (
            (sourceActions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L
        ) {
            actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        }

        if (
            (sourceActions and PlaybackState.ACTION_SEEK_TO) != 0L
        ) {
            actions = actions or PlaybackState.ACTION_SEEK_TO
        }

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

        session.isActive = true

        showNotification(
            title,
            artist,
            artwork,
            state
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "LOCKWAVE Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Current music playback"
                setShowBadge(false)
                lockscreenVisibility =
                    Notification.VISIBILITY_PUBLIC
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
        artwork: Bitmap?,
        state: Int
    ) {

        val playing = state == PlaybackState.STATE_PLAYING

        val notification = Notification.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(artwork)
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

        notificationManager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    fun release() {
        sourceController = null
        session.isActive = false
        session.release()
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
