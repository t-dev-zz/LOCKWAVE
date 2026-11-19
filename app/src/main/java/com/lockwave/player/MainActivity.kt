
package com.lockwave.player

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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
    private lateinit var heroIcon: TextView
    private lateinit var playButton: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var elapsedView: TextView
    private lateinit var durationView: TextView

    private var isDragging = false
    private var lastVisualPlayingState: Boolean? = null
    private var pulseAnimator: ObjectAnimator? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun glass(
        color: Int,
        stroke: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
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

        if (position != null) {
            intent.putExtra(
                MusicSessionManager.EXTRA_POSITION,
                position
            )
        }

        sendBroadcast(intent)
    }

    private fun controlButton(
        symbol: String,
        size: Float,
        width: Int,
        height: Int,
        primary: Boolean,
        action: String
    ): TextView {
        return makeText(symbol, size, white, true).apply {
            gravity = Gravity.CENTER
            background = if (primary) {
                glass(
                    Color.rgb(40, 111, 224),
                    Color.rgb(135, 195, 255),
                    50
                )
            } else {
                glass(
                    Color.argb(55, 100, 140, 195),
                    Color.argb(95, 155, 195, 255),
                    50
                )
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                sendMusicAction(action, source = this)
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(width),
                dp(height)
            )
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
        return String.format(
            java.util.Locale.US,
            "%d:%02d",
            seconds / 60L,
            seconds % 60L
        )
    }

    private val refreshPlayer = object : Runnable {
        override fun run() {
            updatePlayerUi()
            handler.postDelayed(this, 350L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        requestNotificationPermission()

        buildInterface()
        updatePlayerUi()
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
            setPadding(dp(22), dp(34), dp(22), dp(24))

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(17, 37, 72),
                    bg,
                    Color.rgb(8, 19, 39)
                )
            )
        }

        // HEADER

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
            "4.0",
            11f,
            cyan,
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = glass(
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

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        )

        gap(root, 38)

        // PLAYER CARD

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(21), dp(25), dp(21), dp(25))

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.argb(205, 35, 58, 94),
                    Color.argb(175, 18, 34, 61),
                    Color.argb(195, 24, 42, 72)
                )
            ).apply {
                cornerRadius = dp(32).toFloat()
                setStroke(
                    dp(1),
                    Color.argb(100, 145, 190, 255)
                )
            }

            elevation = dp(15).toFloat()
        }

        val liveLabel = makeText(
            "●  LOCKWAVE LIVE",
            10f,
            cyan,
            true
        ).apply {
            letterSpacing = 0.16f
            background = glass(
                Color.argb(35, 90, 160, 255),
                Color.argb(75, 125, 185, 255),
                30
            )
            setPadding(dp(13), dp(8), dp(13), dp(8))
        }

        card.addView(liveLabel)

        gap(card, 27)

        heroIcon = makeText(
            "♫",
            86f,
            blue,
            true
        ).apply {
            gravity = Gravity.CENTER
            setShadowLayer(
                dp(24).toFloat(),
                0f,
                0f,
                Color.argb(190, 45, 125, 255)
            )
        }

        card.addView(
            heroIcon,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(115)
            )
        )

        gap(card, 20)

        titleView = makeText(
            "Nothing playing",
            24f,
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
                topMargin = dp(9)
            }
        )

        gap(card, 18)

        statusView = makeText(
            "WAITING FOR MUSIC",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.17f
        }

        card.addView(statusView)

        gap(card, 27)

        // PROGRESS

        seekBar = SeekBar(this).apply {
            max = 1000
            progress = 0
            isEnabled = false

            progressTintList = ColorStateList.valueOf(blue)
            thumbTintList = ColorStateList.valueOf(cyan)
            progressBackgroundTintList =
                ColorStateList.valueOf(
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
                            val position =
                                duration * seekBar.progress / 1000L

                            sendMusicAction(
                                MusicSessionManager.ACTION_SEEK,
                                position,
                                seekBar
                            )
                        }

                        isDragging = false
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

        elapsedView = makeText("0:00", 11f, muted, true)
        durationView = makeText("0:00", 11f, muted, true)

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

        val separator = View(this).apply {
            setBackgroundColor(
                Color.argb(65, 165, 195, 240)
            )
        }

        card.addView(
            separator,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        gap(card, 24)

        // CONTROLS

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        controls.addView(
            controlButton(
                "|◀", 22f, 55, 55, false,
                MusicSessionManager.ACTION_PREVIOUS
            )
        )

        controls.addView(
            View(this),
            LinearLayout.LayoutParams(dp(20), 1)
        )

        playButton = controlButton(
            "▶", 27f, 72, 72, true,
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
            controlButton(
                "▶|", 22f, 55, 55, false,
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

        gap(root, 28)

        val footer = makeText(
            "MADE FOR THE MUSIC.",
            10f,
            muted,
            true
        ).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.2f
        }

        root.addView(
            footer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun updatePlayerUi() {

        val title = MusicListenerService.currentTitle
        val artist = MusicListenerService.currentArtist
        val playing = MusicListenerService.isPlaying

        val duration = MusicListenerService.currentDuration
        val basePosition = MusicListenerService.currentPosition
        val updateTime = MusicListenerService.lastPositionUpdateTime
        val speed = MusicListenerService.playbackSpeed

        if (title == "Nothing playing") {

            titleView.text = "Nothing playing"
            artistView.text = "Your next song will appear here."

            heroIcon.text = "♫"

            statusView.text = "WAITING FOR MUSIC"
            statusView.setTextColor(muted)

            seekBar.isEnabled = false
            seekBar.progress = 0

            elapsedView.text = "0:00"
            durationView.text = "0:00"

            playButton.text = "▶"

        } else {

            titleView.text = title

            artistView.text = artist.ifBlank {
                MusicListenerService.currentAlbum.ifBlank {
                    "Unknown artist"
                }
            }

            heroIcon.text = if (playing) "♫" else "Ⅱ"

            statusView.text = if (playing) {
                "●  NOW PLAYING"
            } else {
                "Ⅱ  PAUSED"
            }

            statusView.setTextColor(
                if (playing) cyan else muted
            )

            playButton.text = if (playing) "Ⅱ" else "▶"

            val estimatedPosition = if (
                playing && updateTime > 0L
            ) {
                basePosition +
                    (
                        (SystemClock.elapsedRealtime() - updateTime) *
                            speed
                    ).toLong()
            } else {
                basePosition
            }

            val position = estimatedPosition.coerceIn(
                0L,
                duration.coerceAtLeast(0L)
            )

            if (!isDragging) {
                seekBar.isEnabled = duration > 0L

                seekBar.progress = if (duration > 0L) {
                    ((position * 1000L) / duration)
                        .toInt()
                        .coerceIn(0, 1000)
                } else {
                    0
                }

                elapsedView.text = formatTime(position)
            }

            durationView.text = formatTime(duration)
        }

        updatePlayingAnimation(playing)
    }

    private fun updatePlayingAnimation(playing: Boolean) {

        if (lastVisualPlayingState == playing) return
        lastVisualPlayingState = playing

        pulseAnimator?.cancel()
        pulseAnimator = null
        heroIcon.translationY = 0f

        if (playing) {
            pulseAnimator = ObjectAnimator.ofFloat(
                heroIcon,
                "translationY",
                0f,
                -dp(5).toFloat(),
                0f
            ).apply {
                duration = 2300L
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
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
        pulseAnimator?.cancel()
        pulseAnimator = null
        super.onDestroy()
    }
}
