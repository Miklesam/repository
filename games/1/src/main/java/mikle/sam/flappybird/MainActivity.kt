package mikle.sam.flappybird

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import mikle.sam.flappybird.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), FlappyBirdView.OnGameStateChangeListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var scoreText: TextView
    private lateinit var startText: TextView
    private lateinit var gameOverText: TextView
    private lateinit var restartText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system bars for immersive full-screen experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
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
            startText.visibility = View.GONE
            gameOverText.visibility = View.GONE
            restartText.visibility = View.GONE
            scoreText.visibility = View.VISIBLE
        }
    }
    
    override fun onGameOver(score: Int, bestScore: Int) {
        runOnUiThread {
            gameOverText.visibility = View.VISIBLE
            restartText.visibility = View.VISIBLE
            startText.visibility = View.GONE
        }
    }
    
    override fun onGameRestarted() {
        updateUI()
    }
    
    private fun updateUI() {
        scoreText.text = getString(R.string.score, 0)
        scoreText.visibility = View.VISIBLE
        startText.visibility = View.VISIBLE
        gameOverText.visibility = View.GONE
        restartText.visibility = View.GONE
    }
}
