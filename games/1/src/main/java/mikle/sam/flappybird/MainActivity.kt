package mikle.sam.flappybird

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import mikle.sam.flappybird.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), FlappyBirdView.OnGameStateChangeListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var scoreText: TextView
    private lateinit var startText: TextView
    private lateinit var gameOverText: TextView
    private lateinit var restartText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize views
        scoreText = binding.scoreText
        startText = binding.startText
        gameOverText = binding.gameOverText
        restartText = binding.restartText
        
        // Set up game view
        binding.flappyBirdView.setOnGameStateChangeListener(this)
        
        // Initialize UI
        updateUI()
    }
    
    override fun onScoreChanged(score: Int) {
        scoreText.text = getString(R.string.score, score)
    }
    
    override fun onGameStarted() {
        runOnUiThread {
            startText.visibility = GONE
            gameOverText.visibility = GONE
            restartText.visibility = GONE
            scoreText.visibility = VISIBLE
        }
    }
    
    override fun onGameOver(score: Int, bestScore: Int) {
        runOnUiThread {
            gameOverText.visibility = VISIBLE
            restartText.visibility = VISIBLE
            startText.visibility = GONE
        }
    }
    
    override fun onGameRestarted() {
        updateUI()
    }
    
    private fun updateUI() {
        scoreText.text = getString(R.string.score, 0)
        scoreText.visibility = VISIBLE
        startText.visibility = VISIBLE
        gameOverText.visibility = GONE
        restartText.visibility = GONE
    }
}
