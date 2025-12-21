package mikle.sam.bubblepop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import mikle.sam.bubblepop.databinding.ActivityMainBinding
import mikle.sam.bubblepop.ui.ShooterGameView

class MainActivity : AppCompatActivity(), ShooterGameView.Listener {

    private lateinit var binding: ActivityMainBinding
    private var gameStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameView.setListener(this)
        binding.playButton.setOnClickListener {
            startGame()
        }
    }

    override fun onResume() {
        super.onResume()
        if (gameStarted) {
            binding.gameView.resumeGame()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.pauseGame()
    }

    override fun onDestroy() {
        binding.gameView.setListener(null)
        super.onDestroy()
    }

    private fun startGame() {
        gameStarted = true
        binding.statusText.isVisible = false
        binding.playButton.isVisible = false
        binding.gameView.startGame()
    }

    override fun onScoreChanged(score: Int) {
        binding.scoreText.text = getString(R.string.score_label, score)
    }

    override fun onLivesChanged(lives: Int) {
        binding.livesText.text = getString(R.string.lives_label, lives)
    }

    override fun onLevelChanged(level: Int) {
        binding.levelText.text = getString(R.string.level_label, level)
    }

    override fun onComboChanged(combo: Int) {
        if (combo > 0) {
            binding.comboText.apply {
                isVisible = true
                text = getString(R.string.combo_label, combo)
            }
        } else {
            binding.comboText.isVisible = false
        }
    }

    override fun onGameOver(finalScore: Int) {
        gameStarted = false
        binding.statusText.apply {
            isVisible = true
            text = getString(R.string.game_over_message, finalScore)
        }
        binding.playButton.apply {
            isVisible = true
            text = getString(R.string.play_again)
        }
        binding.comboText.isVisible = false
    }
}

