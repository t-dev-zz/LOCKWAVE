
package com.lockwave.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val backgroundColor = Color.rgb(8, 13, 24)
    private val blue = Color.rgb(57, 133, 255)
    private val muted = Color.rgb(137, 151, 177)
    private val cardColor = Color.rgb(19, 29, 49)

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var musicIcon: TextView
    private lateinit var trackTitle: TextView
    private lateinit var trackArtist: TextView
    private lateinit var playbackStatus: TextView

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun text(
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
                Typeface.DEFAULT
            }
        }
    }

    private val refreshPlayer = object : Runnable {
        override fun run() {
            updatePlayerUi()
            handler.postDelayed(this, 700)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(35), dp(24), dp(24))
            setBackgroundColor(backgroundColor)
        }

        val brand = text("LOCKWAVE", 25f, Color.WHITE, true)
        brand.letterSpacing = 0.16f

        root.addView(
            brand,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = text(
            "YOUR MUSIC. YOUR LOCK SCREEN.",
            10f,
            muted,
            true
        ).apply {
            letterSpacing = 0.18f
        }

        val subtitleParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(10)
        }

        root.addView(subtitle, subtitleParams)

        val spacer = View(this)

        root.addView(
            spacer,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        val playerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(30), dp(22), dp(30))
            background = rounded(cardColor, 28)
        }

        musicIcon = text(
            "♫",
            76f,
            blue,
            true
        ).apply {
            gravity = Gravity.CENTER
        }

        playerCard.addView(
            musicIcon,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(125)
            )
        )

        trackTitle = text(
            "Nothing playing",
            21f,
            Color.WHITE,
            true
        ).apply {
            gravity = Gravity.CENTER
            maxLines = 2
        }

        val titleParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(20)
        }

        playerCard.addView(trackTitle, titleParams)

        trackArtist = text(
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
            topMargin = dp(9)
        }

        playerCard.addView(trackArtist, artistParams)

        playbackStatus = text(
            "WAITING FOR MUSIC",
            10f,
            muted,
            true
        ).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
        }

        val statusParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(25)
        }

        playerCard.addView(playbackStatus, statusParams)

        root.addView(
            playerCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(285)
            )
        )

        val spacerBottom = View(this)

        root.addView(
            spacerBottom,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        val footer = text(
            "MADE FOR THE MUSIC.",
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

            trackTitle.text = "Nothing playing"
            trackArtist.text = "Your next song will appear here."

            musicIcon.text = "♫"
            musicIcon.setTextColor(blue)

            playbackStatus.text = "WAITING FOR MUSIC"
            playbackStatus.setTextColor(muted)

        } else {

            trackTitle.text = title

            trackArtist.text = if (artist.isNotBlank()) {
                artist
            } else {
                MusicListenerService.currentAlbum.ifBlank {
                    "Unknown artist"
                }
            }

            musicIcon.text = if (playing) "♫" else "Ⅱ"
            musicIcon.setTextColor(blue)

            playbackStatus.text = if (playing) {
                "NOW PLAYING"
            } else {
                "PAUSED"
            }

            playbackStatus.setTextColor(
                if (playing) blue else muted
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
