
package com.lockwave.player

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

    private val bg = Color.rgb(6, 11, 23)
    private val blue = Color.rgb(72, 145, 255)
    private val cyan = Color.rgb(120, 211, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(143, 159, 185)

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

    private fun gap(
        parent: LinearLayout,
        height: Int
    ) {
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

        val button = makeText(
            symbol,
            size,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = if (primary) {
                glass(
                    Color.rgb(43, 111, 220),
                    Color.rgb(125, 190, 255),
                    50
                )
            } else {
                glass(
                    Color.argb(50, 100, 135, 185),
                    Color.argb(80, 170, 200, 255),
                    50
                )
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                sendMusicAction(action, source = this)
            }
        }

        button.layoutParams = LinearLayout.LayoutParams(
            dp(width),
            dp(height)
        )

        return button
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L

        return String.format(
            java.util.Locale.US,
            "%d:%02d",
            minutes,
            seconds
        )
    }

    private val refreshPlayer = object : Runnable {
        override fun run() {
            updatePlayerUi()
            handler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bg)
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(23), dp(30), dp(23), dp(22))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(14, 28, 53),
                    bg,
                    Color.rgb(8, 16, 32)
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
            letterSpacing = 0.15f
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
            "3.0",
            11f,
            cyan,
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(7), dp(13), dp(7))
            background = glass(
                Color.argb(40, 76, 151, 255),
                Color.argb(100, 76, 151, 255),
                30
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
            letterSpacing = 0.17f
        }

        val subtitleParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(9)
        }

        root.addView(subtitle, subtitleParams)

        gap(root, 34)

        // MAIN PLAYER CARD

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(21), dp(23), dp(21), dp(23))
            background = glass(
                Color.argb(165, 24, 40, 68),
                Color.argb(75, 155, 195, 255),
                30
            )
            elevation = dp(12).toFloat()
        }

        val liveLabel = makeText(
            "●  LIVE SESSION",
            10f,
            cyan,
            true
        ).apply {
            letterSpacing = 0.17f
        }

        card.addView(liveLabel)

        gap(card, 22)

        heroIcon = makeText(
            "♫",
            82f,
            blue,
            true
        ).apply {
            gravity = Gravity.CENTER
            setShadowLayer(
                dp(22).toFloat(),
                0f,
                0f,
                Color.argb(160, 45, 125, 255)
            )
        }

        card.addView(
            heroIcon,
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

        val artistParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
        }

        card.addView(artistView, artistParams)

        gap(card, 17)

        statusView = makeText(
            "WAITING FOR MUSIC",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.17f
        }

        card.addView(statusView)

        gap(card, 25)

        // PROGRESS BAR

        seekBar = SeekBar(this).apply {
            max = 1000
            progress = 0
            isEnabled = false

            progressTintList = ColorStateList.valueOf(blue)
            thumbTintList = ColorStateList.valueOf(cyan)
            progressBackgroundTintList =
                ColorStateList.valueOf(
                    Color.argb(75, 170, 195, 230)
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

                            val position =
                                duration * progress / 1000L

                            elapsedView.text = formatTime(position)
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
                dp(35)
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

        gap(card, 23)

        val separator = View(this).apply {
            setBackgroundColor(
                Color.argb(50, 170, 200, 255)
            )
        }

        card.addView(
            separator,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        gap(card, 22)

        // PLAYBACK CONTROLS

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val previous = controlButton(
            "|◀",
            22f,
            55,
            55,
            false,
            MusicSessionManager.ACTION_PREVIOUS
        )

        controls.addView(previous)

        controls.addView(
            View(this),
            LinearLayout.LayoutParams(dp(22), 1)
        )

        playButton = controlButton(
            "▶",
            27f,
            70,
            70,
            true,
            MusicSessionManager.ACTION_PLAY
        )

        controls.addView(playButton)

        controls.addView(
            View(this),
            LinearLayout.LayoutParams(dp(22), 1)
        )

        val next = controlButton(
            "▶|",
            22f,
            55,
            55,
            false,
            MusicSessionManager.ACTION_NEXT
        )

        controls.addView(next)

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
            "CRAFTED FOR THE SOUND.",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
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

        updatePlayerUi()
    }

    private fun updatePlayerUi() {

        val title = MusicListenerService.currentTitle
        val artist = MusicListenerService.currentArtist
        val playing = MusicListenerService.isPlaying

        val duration =
            MusicListenerService.currentDuration

        val basePosition =
            MusicListenerService.currentPosition

        val updateTime =
            MusicListenerService.lastPositionUpdateTime

        val speed =
            MusicListenerService.playbackSpeed

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
