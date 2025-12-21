package mikle.sam.brickbreaker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import mikle.sam.brickbreaker.databinding.ActivityMainBinding
import mikle.sam.brickbreaker.ui.BrickBreakerView

class MainActivity : AppCompatActivity(), BrickBreakerView.Listener {

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

    override fun onGameOver(finalScore: Int, finalLevel: Int) {
        gameStarted = false
        binding.statusText.apply {
            isVisible = true
            text = getString(R.string.game_over_message, finalScore, finalLevel)
        }
        binding.playButton.apply {
            isVisible = true
            text = getString(R.string.play_again)
        }
    }

    override fun onLevelComplete(level: Int) {
        binding.statusText.apply {
            isVisible = true
            text = getString(R.string.level_complete, level)
        }
        binding.playButton.apply {
            isVisible = true
            text = getString(R.string.next_level)
            setOnClickListener {
                // Продолжаем игру на следующем уровне
                binding.statusText.isVisible = false
                binding.playButton.isVisible = false
                gameStarted = true
                binding.gameView.resumeGame()
            }
        }
    }
}

