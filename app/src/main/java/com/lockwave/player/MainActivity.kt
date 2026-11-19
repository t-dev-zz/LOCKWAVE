
package com.lockwave.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val bg = Color.rgb(5, 10, 23)
    private val blue = Color.rgb(69, 145, 255)
    private val cyan = Color.rgb(133, 216, 255)
    private val white = Color.rgb(246, 249, 255)
    private val muted = Color.rgb(143, 160, 188)

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var statusView: TextView
    private lateinit var playButton: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var elapsedView: TextView
    private lateinit var durationView: TextView

    private var isDragging = false

    private var lastTitle = ""
    private var lastArtist = ""
    private var lastPlaying: Boolean? = null
    private var lastDuration = -1L
    private var lastProgress = -1
    private var lastStatus = ""

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        stroke: Int = Color.TRANSPARENT,
        radius: Int = 24
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (stroke != Color.TRANSPARENT) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            typeface = if (bold) {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }
    }

    private fun gap(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds.coerceAtLeast(0L) / 1000L

        return String.format(
            java.util.Locale.US,
            "%d:%02d",
            seconds / 60L,
            seconds % 60L
        )
    }

    private fun sendMusicAction(
        action: String,
        position: Long? = null,
        source: View? = null
    ) {
        source?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY
        )

        val intent = Intent(
            this,
            MusicActionReceiver::class.java
        ).setAction(action)

        position?.let {
            intent.putExtra(
                MusicSessionManager.EXTRA_POSITION,
                it
            )
        }

        sendBroadcast(intent)
    }

    private fun makeButton(
        symbol: String,
        width: Int,
        primary: Boolean,
        action: String
    ): TextView {
        return makeText(symbol, 23f, white, true).apply {
            gravity = Gravity.CENTER

            background = rounded(
                if (primary) {
                    Color.rgb(40, 111, 224)
                } else {
                    Color.argb(55, 100, 140, 195)
                },
                if (primary) {
                    Color.rgb(135, 195, 255)
                } else {
                    Color.argb(95, 155, 195, 255)
                },
                50
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                sendMusicAction(action, source = this)
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(width),
                dp(width)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        requestNotificationPermission()
        buildInterface()
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                410
            )
        }
    }

    private fun buildInterface() {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(35), dp(22), dp(25))

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(17, 37, 72),
                    bg,
                    Color.rgb(8, 19, 39)
                )
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = makeText(
            "LOCKWAVE",
            25f,
            white,
            true
        ).apply {
            letterSpacing = 0.12f
        }

        header.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val version = makeText(
            "5.0",
            11f,
            cyan,
            true
        ).apply {
            setPadding(dp(14), dp(8), dp(14), dp(8))

            background = rounded(
                Color.argb(55, 70, 145, 255),
                Color.argb(130, 105, 175, 255),
                40
            )
        }

        header.addView(version)
        root.addView(header)

        val subtitle = makeText(
            "YOUR MUSIC. YOUR LOCK SCREEN.",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.18f
        }

        root.addView(subtitle)
        gap(root, 35)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(21),
                dp(27),
                dp(21),
                dp(25)
            )

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.argb(205, 35, 58, 94),
                    Color.argb(175, 18, 34, 61),
                    Color.argb(195, 24, 42, 72)
                )
            ).apply {
                cornerRadius = dp(30).toFloat()

                setStroke(
                    dp(1),
                    Color.argb(100, 145, 190, 255)
                )
            }

            elevation = dp(12).toFloat()
        }

        val live = makeText(
            "LOCKWAVE 5.0",
            10f,
            cyan,
            true
        ).apply {
            letterSpacing = 0.15f

            setPadding(
                dp(13),
                dp(8),
                dp(13),
                dp(8)
            )

            background = rounded(
                Color.argb(35, 90, 160, 255),
                Color.argb(75, 125, 185, 255),
                30
            )
        }

        card.addView(live)
        gap(card, 28)

        val musicSymbol = makeText(
            "♫",
            76f,
            blue,
            true
        ).apply {
            gravity = Gravity.CENTER
        }

        card.addView(
            musicSymbol,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(105)
            )
        )

        gap(card, 18)

        titleView = makeText(
            "Nothing playing",
            23f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            maxLines = 2
        }

        card.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        artistView = makeText(
            "Your next song will appear here.",
            13f,
            muted
        ).apply {
            gravity = Gravity.CENTER
            maxLines = 1
        }

        card.addView(
            artistView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        gap(card, 18)

        statusView = makeText(
            "WAITING FOR MUSIC",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.15f
        }

        card.addView(statusView)
        gap(card, 24)

        seekBar = SeekBar(this).apply {
            max = 1000
            isEnabled = false

            progressTintList =
                android.content.res.ColorStateList.valueOf(blue)

            thumbTintList =
                android.content.res.ColorStateList.valueOf(cyan)

            progressBackgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.argb(80, 165, 195, 235)
                )

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        bar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            val duration =
                                MusicListenerService.currentDuration

                            elapsedView.text = formatTime(
                                duration * progress / 1000L
                            )
                        }
                    }

                    override fun onStartTrackingTouch(
                        bar: SeekBar?
                    ) {
                        isDragging = true
                    }

                    override fun onStopTrackingTouch(
                        bar: SeekBar?
                    ) {
                        val duration =
                            MusicListenerService.currentDuration

                        if (duration > 0L) {
                            val target =
                                duration * seekBar.progress / 1000L

                            sendMusicAction(
                                MusicSessionManager.ACTION_SEEK,
                                target,
                                seekBar
                            )
                        }

                        isDragging = false
                        lastProgress = -1
                    }
                }
            )
        }

        card.addView(
            seekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        elapsedView = makeText(
            "0:00",
            11f,
            muted,
            true
        )

        durationView = makeText(
            "0:00",
            11f,
            muted,
            true
        )

        timeRow.addView(
            elapsedView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        timeRow.addView(durationView)
        card.addView(timeRow)

        gap(card, 25)

        val divider = View(this).apply {
            setBackgroundColor(
                Color.argb(65, 165, 195, 240)
            )
        }

        card.addView(
            divider,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        gap(card, 23)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        controls.addView(
            makeButton(
                "|◀",
                55,
                false,
                MusicSessionManager.ACTION_PREVIOUS
            )
        )

        controls.addView(
            View(this),
            LinearLayout.LayoutParams(dp(20), 1)
        )

        playButton = makeButton(
            "▶",
            70,
            true,
            MusicSessionManager.ACTION_PLAY
        )

        playButton.setOnClickListener {
            sendMusicAction(
                if (MusicListenerService.isPlaying) {
                    MusicSessionManager.ACTION_PAUSE
                } else {
                    MusicSessionManager.ACTION_PLAY
                },
                source = playButton
            )
        }

        controls.addView(playButton)

        controls.addView(
            View(this),
            LinearLayout.LayoutParams(dp(20), 1)
        )

        controls.addView(
            makeButton(
                "▶|",
                55,
                false,
                MusicSessionManager.ACTION_NEXT
            )
        )

        card.addView(controls)

        root.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        gap(root, 24)

        val accessButton = makeText(
            "OPEN NOTIFICATION ACCESS SETTINGS",
            11f,
            cyan,
            true
        ).apply {
            gravity = Gravity.CENTER

            setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
            )

            background = rounded(
                Color.argb(35, 90, 160, 255),
                Color.argb(100, 105, 175, 255),
                18
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                try {
                    startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                } catch (_: Exception) {
                    startActivity(
                        Intent(Settings.ACTION_SETTINGS)
                    )
                }
            }
        }

        root.addView(
            accessButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        gap(root, 20)

        val footer = makeText(
            "MADE FOR THE MUSIC.",
            10f,
            muted,
            true
        ).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
        }

        root.addView(footer)

        scroll.addView(root)
        setContentView(scroll)

        updatePlayerUi()
    }

    private val refreshPlayer = object : Runnable {
        override fun run() {
            updatePlayerUi()
            handler.postDelayed(this, 500L)
        }
    }

    private fun updatePlayerUi() {
        val title = MusicListenerService.currentTitle
        val artist = MusicListenerService.currentArtist
        val playing = MusicListenerService.isPlaying
        val duration = MusicListenerService.currentDuration

        if (title != lastTitle) {
            lastTitle = title
            titleView.text = title
        }

        val displayArtist = if (title == "Nothing playing") {
            "Your next song will appear here."
        } else {
            artist.ifBlank {
                MusicListenerService.currentAlbum.ifBlank {
                    "Unknown artist"
                }
            }
        }

        if (displayArtist != lastArtist) {
            lastArtist = displayArtist
            artistView.text = displayArtist
        }

        // Update playback button only when playback changes.
        if (playing != lastPlaying) {
            lastPlaying = playing

            playButton.text = if (playing) "Ⅱ" else "▶"
        }

        // Update status independently of playback state.
        // This fixes the new-track / unchanged-playing-state bug.
        val newStatus = when {
            title == "Nothing playing" -> "WAITING FOR MUSIC"
            playing -> "NOW PLAYING"
            else -> "PAUSED"
        }

        if (newStatus != lastStatus) {
            lastStatus = newStatus
            statusView.text = newStatus
        }

        statusView.setTextColor(
            if (playing && title != "Nothing playing") cyan else muted
        )

        if (duration != lastDuration) {
            lastDuration = duration
            durationView.text = formatTime(duration)
            seekBar.isEnabled = duration > 0L
        }

        if (title == "Nothing playing") {
            if (!isDragging && seekBar.progress != 0) {
                seekBar.progress = 0
                lastProgress = 0
            }

            elapsedView.text = "0:00"
            return
        }

        val basePosition = MusicListenerService.currentPosition
        val updateTime = MusicListenerService.lastPositionUpdateTime
        val speed = MusicListenerService.playbackSpeed

        val estimatedPosition = if (
            playing && updateTime > 0L
        ) {
            basePosition + (
                (SystemClock.elapsedRealtime() - updateTime) * speed
            ).toLong()
        } else {
            basePosition
        }

        val position = estimatedPosition.coerceIn(
            0L,
            duration.coerceAtLeast(0L)
        )

        if (!isDragging && duration > 0L) {
            val progress = (
                position * 1000L / duration
            ).toInt().coerceIn(0, 1000)

            if (progress != lastProgress) {
                lastProgress = progress
                seekBar.progress = progress
            }

            elapsedView.text = formatTime(position)
        }
    }

    override fun onResume() {
        super.onResume()

        handler.removeCallbacks(refreshPlayer)
        handler.post(refreshPlayer)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshPlayer)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshPlayer)
        super.onDestroy()
    }
}
