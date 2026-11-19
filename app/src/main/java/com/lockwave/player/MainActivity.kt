
package com.lockwave.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val bg = Color.rgb(5, 9, 19)
    private val blue = Color.rgb(76, 151, 255)
    private val cyan = Color.rgb(115, 207, 255)
    private val muted = Color.rgb(143, 159, 185)
    private val white = Color.WHITE

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var titleView: TextView
    private lateinit var artistView: TextView
    private lateinit var statusView: TextView
    private lateinit var playButton: TextView
    private lateinit var heroIcon: TextView

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

    private fun addGap(
        parent: LinearLayout,
        height: Int
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun sendMusicAction(action: String, view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY
        )

        val intent = Intent(
            this,
            MusicActionReceiver::class.java
        ).setAction(action)

        sendBroadcast(intent)
    }

    private fun controlButton(
        symbol: String,
        size: Float,
        background: GradientDrawable,
        action: String,
        width: Int,
        height: Int
    ): TextView {
        return makeText(symbol, size, white, true).apply {
            gravity = Gravity.CENTER
            this.background = background
            isClickable = true
            isFocusable = true

            setOnClickListener {
                sendMusicAction(action, this)
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(width),
                dp(height)
            )
        }
    }

    private val refreshPlayer = object : Runnable {
        override fun run() {
            updatePlayerUi()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(35), dp(24), dp(20))
            setBackground(
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        Color.rgb(13, 25, 48),
                        bg,
                        Color.rgb(8, 16, 32)
                    )
                )
            )
        }

        // Header

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = makeText(
            "LOCKWAVE",
            24f,
            white,
            true
        ).apply {
            letterSpacing = 0.16f
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
            "2.0",
            11f,
            cyan,
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(7), dp(13), dp(7))
            background = glass(
                Color.argb(35, 76, 151, 255),
                Color.argb(90, 76, 151, 255),
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

        // Flexible space

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        // Player glass panel

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(25), dp(22), dp(25))
            background = glass(
                Color.argb(155, 24, 39, 66),
                Color.argb(65, 155, 195, 255),
                30
            )
            elevation = dp(12).toFloat()
        }

        // Live indicator

        val liveLabel = makeText(
            "●  LIVE SESSION",
            10f,
            cyan,
            true
        ).apply {
            letterSpacing = 0.16f
        }

        card.addView(liveLabel)

        addGap(card, 24)

        // Music symbol

        heroIcon = makeText(
            "♫",
            86f,
            blue,
            true
        ).apply {
            gravity = Gravity.CENTER
            setShadowLayer(
                dp(22).toFloat(),
                0f,
                0f,
                Color.argb(150, 45, 125, 255)
            )
        }

        card.addView(
            heroIcon,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(112)
            )
        )

        addGap(card, 18)

        // Track information

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

        val artistParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
        }

        artistView = makeText(
            "Your next song will appear here.",
            13f,
            muted
        ).apply {
            gravity = Gravity.CENTER
            maxLines = 1
        }

        card.addView(artistView, artistParams)

        addGap(card, 20)

        // Playback status

        statusView = makeText(
            "WAITING FOR MUSIC",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.18f
        }

        card.addView(statusView)

        addGap(card, 23)

        // Separator

        val separator = View(this).apply {
            setBackgroundColor(
                Color.argb(45, 170, 200, 255)
            )
        }

        card.addView(
            separator,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        addGap(card, 20)

        // Playback controls

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val previous = controlButton(
            "|◀",
            23f,
            glass(
                Color.argb(45, 100, 135, 185),
                Color.argb(70, 170, 200, 255),
                50
            ),
            MusicSessionManager.ACTION_PREVIOUS,
            54,
            54
        )

        controls.addView(previous)

        val controlGap1 = View(this)
        controls.addView(
            controlGap1,
            LinearLayout.LayoutParams(dp(24), 1)
        )

        playButton = controlButton(
            "▶",
            27f,
            glass(
                Color.rgb(44, 112, 220),
                Color.rgb(110, 180, 255),
                50
            ),
            MusicSessionManager.ACTION_PLAY,
            70,
            70
        )

        controls.addView(playButton)

        val controlGap2 = View(this)
        controls.addView(
            controlGap2,
            LinearLayout.LayoutParams(dp(24), 1)
        )

        val next = controlButton(
            "▶|",
            23f,
            glass(
                Color.argb(45, 100, 135, 185),
                Color.argb(70, 170, 200, 255),
                50
            ),
            MusicSessionManager.ACTION_NEXT,
            54,
            54
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

        // Bottom section

        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, 0, 1f)
        )

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

        setContentView(root)

        updatePlayerUi()
    }

    private fun updatePlayerUi() {

        val title = MusicListenerService.currentTitle
        val artist = MusicListenerService.currentArtist
        val playing = MusicListenerService.isPlaying

        if (title == "Nothing playing") {

            titleView.text = "Nothing playing"
            artistView.text = "Your next song will appear here."

            heroIcon.text = "♫"
            statusView.text = "WAITING FOR MUSIC"
            statusView.setTextColor(muted)

            playButton.text = "▶"
            playButton.setOnClickListener {
                sendMusicAction(
                    MusicSessionManager.ACTION_PLAY,
                    playButton
                )
            }

        } else {

            titleView.text = title

            artistView.text = if (artist.isNotBlank()) {
                artist
            } else {
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

            playButton.setOnClickListener {
                sendMusicAction(
                    if (MusicListenerService.isPlaying) {
                        MusicSessionManager.ACTION_PAUSE
                    } else {
                        MusicSessionManager.ACTION_PLAY
                    },
                    playButton
                )
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
        super.onDestroy()
    }
}
