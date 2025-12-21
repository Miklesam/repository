package mikle.sam.spacerunner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import mikle.sam.spacerunner.databinding.ActivityMainBinding
import mikle.sam.spacerunner.ui.SpaceRunnerView

class MainActivity : AppCompatActivity(), SpaceRunnerView.Listener {

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

    override fun onDistanceChanged(distance: Int) {
        binding.distanceText.text = getString(R.string.distance_label, distance)
    }

    override fun onSpeedChanged(speed: Int) {
        binding.speedText.text = getString(R.string.speed_label, speed)
    }

    override fun onGameOver(finalScore: Int, finalDistance: Int) {
        gameStarted = false
        binding.statusText.apply {
            isVisible = true
            text = getString(R.string.game_over_message, finalScore, finalDistance)
        }
        binding.playButton.apply {
            isVisible = true
            text = getString(R.string.play_again)
        }
    }
}


