package mikle.sam.towerdefense

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import mikle.sam.towerdefense.databinding.ActivityMainBinding
import mikle.sam.towerdefense.ui.TowerDefenseView

class MainActivity : AppCompatActivity(), TowerDefenseView.GameListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameView.setGameListener(this)

        binding.startWaveButton.setOnClickListener {
            binding.gameView.startWave()
            binding.startWaveButton.isEnabled = false
        }

        binding.pauseButton.setOnClickListener {
            if (binding.gameView.isPaused()) {
                binding.gameView.resumeGame()
                binding.pauseButton.text = getString(R.string.pause)
            } else {
                binding.gameView.pauseGame()
                binding.pauseButton.text = getString(R.string.resume)
            }
        }

        binding.restartButton.setOnClickListener {
            binding.gameView.resetGame()
            binding.gameOverText.isVisible = false
            binding.restartButton.isVisible = false
            binding.startWaveButton.isEnabled = true
            binding.upgradePanel.isVisible = false
        }

        binding.upgradeButton.setOnClickListener {
            val upgraded = binding.gameView.upgradeSelectedTower()
            if (upgraded) {
                updateUpgradePanel()
            }
        }

        // Speed control buttons
        binding.speed1xButton.setOnClickListener {
            binding.gameView.setGameSpeed(1.0f)
            updateSpeedDisplay()
        }

        binding.speed2xButton.setOnClickListener {
            binding.gameView.setGameSpeed(2.0f)
            updateSpeedDisplay()
        }

        binding.speed3xButton.setOnClickListener {
            binding.gameView.setGameSpeed(3.0f)
            updateSpeedDisplay()
        }

        // Initialize speed display
        updateSpeedDisplay()
    }

    override fun onResume() {
        super.onResume()
        binding.gameView.resumeGame()
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.pauseGame()
    }

    override fun onDestroy() {
        binding.gameView.setGameListener(null)
        super.onDestroy()
    }

    override fun onWaveChanged(wave: Int) {
        binding.waveText.text = getString(R.string.wave, wave)
    }

    override fun onGoldChanged(gold: Int) {
        binding.goldText.text = getString(R.string.gold, gold)
        updateUpgradePanel()
    }

    override fun onLivesChanged(lives: Int) {
        binding.livesText.text = getString(R.string.lives, lives)
    }

    override fun onGameOver(victory: Boolean) {
        binding.gameOverText.apply {
            isVisible = true
            text = if (victory) getString(R.string.victory) else getString(R.string.game_over)
        }
        binding.restartButton.isVisible = true
    }

    override fun onWaveComplete() {
        binding.startWaveButton.isEnabled = true
    }

    override fun onTowerSelected(tower: mikle.sam.towerdefense.model.Tower?) {
        if (tower != null) {
            binding.upgradePanel.isVisible = true
            updateUpgradePanel()
        } else {
            binding.upgradePanel.isVisible = false
        }
    }

    private fun updateUpgradePanel() {
        val tower = binding.gameView.getSelectedTower() ?: return
        
        val levelText = if (tower.canUpgrade()) {
            getString(R.string.tower_level, tower.level)
        } else {
            getString(R.string.max_level)
        }
        
        val costText = if (tower.canUpgrade()) {
            getString(R.string.upgrade_cost, tower.getUpgradeCost())
        } else {
            ""
        }
        
        binding.towerInfoText.text = "$levelText\n$costText"
        val canAfford = tower.canUpgrade() && 
            tower.getUpgradeCost() <= binding.gameView.getCurrentGold()
        binding.upgradeButton.isEnabled = canAfford
    }

    private fun updateSpeedDisplay() {
        val speed = binding.gameView.getGameSpeed()
        binding.speedText.text = getString(R.string.game_speed, speed)
        
        // Update button states
        binding.speed1xButton.isSelected = speed == 1.0f
        binding.speed2xButton.isSelected = speed == 2.0f
        binding.speed3xButton.isSelected = speed == 3.0f
    }
}

