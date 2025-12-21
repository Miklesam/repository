package mikle.sam.basketball

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import mikle.sam.basketball.databinding.ActivityMainBinding
import mikle.sam.basketball.ui.BasketballView

class MainActivity : AppCompatActivity(), BasketballView.Listener {

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

    override fun onShotsChanged(shots: Int, made: Int) {
        binding.shotsText.text = getString(R.string.shots_label, made, shots)
    }

    override fun onTimeChanged(timeLeft: Int) {
        binding.timeText.text = getString(R.string.time_label, timeLeft)
    }

    override fun onGameOver(finalScore: Int, shotsMade: Int, totalShots: Int) {
        gameStarted = false
        binding.statusText.apply {
            isVisible = true
            text = getString(R.string.game_over_message, finalScore, shotsMade, totalShots)
        }
        binding.playButton.apply {
            isVisible = true
            text = getString(R.string.play_again)
        }
    }
}


