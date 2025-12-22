package mikle.sam.game0

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var backgroundMusic: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val laneCount = intent.getIntExtra("laneCount", 2)
        val frequency = intent.getIntExtra("frequency", 101-33)
        gameView = GameView(this, null)
        gameView.setGameParameters(laneCount, frequency)
        gameView.setTimedGameMode()
        setContentView(gameView)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.navigationBarColor = Color.WHITE
        
        // Initialize background music
        initializeMusic()
        
        // Set music controller for GameView
        gameView.setMusicController(object : GameView.MusicController {
            override fun pauseMusic() {
                pauseBackgroundMusic()
            }
            
            override fun resumeMusic() {
                resumeBackgroundMusic()
            }
        })
    }

    private fun initializeMusic() {
        try {
            // Try to load music from raw folder
            // Place your music file (e.g., background_music.mp3) in res/raw/ folder
            // and name it background_music (lowercase, no spaces, no special chars except underscore)
            val musicResourceId = resources.getIdentifier("background_music", "raw", packageName)
            if (musicResourceId != 0) {
                backgroundMusic = MediaPlayer.create(this, musicResourceId)
                backgroundMusic?.isLooping = true
                backgroundMusic?.setVolume(0.5f, 0.5f) // Set volume to 50%
            }
        } catch (e: Exception) {
            // Music file not found or error loading - continue without music
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
        // Start music if available, game is not paused, and game is not over
        if (!gameView.isGamePaused() && !gameView.isGameOver()) {
            resumeBackgroundMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        // Pause music when activity is paused
        pauseBackgroundMusic()
    }
    
    private fun pauseBackgroundMusic() {
        try {
            backgroundMusic?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun resumeBackgroundMusic() {
        try {
            backgroundMusic?.let {
                if (!it.isPlaying) {
                    it.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release music resources
        try {
            backgroundMusic?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            backgroundMusic = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}