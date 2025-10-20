package mikle.sam.game2048

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import mikle.sam.game2048.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), Game2048View.OnGameStateChangeListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var scoreText: TextView
    private lateinit var bestScoreText: TextView
    private lateinit var resetButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize views
        scoreText = binding.scoreText
        bestScoreText = binding.bestScoreText
        resetButton = binding.resetButton
        
        // Set up game view
        binding.game2048View.setOnGameStateChangeListener(this)
        
        // Set up reset button
        resetButton.setOnClickListener {
            showResetDialog()
        }
        
        // Initialize UI
        updateScoreDisplay()
    }
    
    override fun onScoreChanged(score: Int) {
        runOnUiThread {
            scoreText.text = getString(R.string.score, score)
            updateBestScore(score)
        }
    }
    
    override fun onGameWon() {
        runOnUiThread {
            showWinDialog()
        }
    }
    
    override fun onGameOver() {
        runOnUiThread {
            showGameOverDialog()
        }
    }
    
    override fun onGameReset() {
        runOnUiThread {
            updateScoreDisplay()
        }
    }
    
    private fun updateScoreDisplay() {
        scoreText.text = getString(R.string.score, 0)
        bestScoreText.text = getString(R.string.best_score, getBestScore())
    }
    
    private fun updateBestScore(score: Int) {
        val bestScore = getBestScore()
        if (score > bestScore) {
            saveBestScore(score)
            bestScoreText.text = getString(R.string.best_score, score)
        }
    }
    
    private fun getBestScore(): Int {
        val prefs = getSharedPreferences("game2048", MODE_PRIVATE)
        return prefs.getInt("best_score", 0)
    }
    
    private fun saveBestScore(score: Int) {
        val prefs = getSharedPreferences("game2048", MODE_PRIVATE)
        prefs.edit().putInt("best_score", score).apply()
    }
    
    private fun showWinDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.you_won)
            .setMessage(R.string.win_message)
            .setPositiveButton(R.string.continue_game) { _, _ -> }
            .setNegativeButton(R.string.new_game) { _, _ ->
                binding.game2048View.resetGame()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.game_over)
            .setMessage(R.string.game_over_message)
            .setPositiveButton(R.string.try_again) { _, _ ->
                binding.game2048View.resetGame()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_game)
            .setMessage(R.string.reset_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                binding.game2048View.resetGame()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }
}

