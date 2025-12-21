package mikle.sam.gravityflow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import mikle.sam.gravityflow.databinding.ActivityMainBinding
import mikle.sam.gravityflow.model.ElementType
import mikle.sam.gravityflow.ui.ZenGardenView

class MainActivity : AppCompatActivity(), ZenGardenView.GameListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gardenView.setGameListener(this)

        // Кнопки выбора элементов
        binding.stoneButton.setOnClickListener {
            binding.gardenView.setSelectedElementType(ElementType.STONE)
            updateButtonSelection(ElementType.STONE)
        }

        binding.flowerButton.setOnClickListener {
            binding.gardenView.setSelectedElementType(ElementType.FLOWER)
            updateButtonSelection(ElementType.FLOWER)
        }

        binding.leafButton.setOnClickListener {
            binding.gardenView.setSelectedElementType(ElementType.LEAF)
            updateButtonSelection(ElementType.LEAF)
        }

        binding.waterButton.setOnClickListener {
            binding.gardenView.setSelectedElementType(ElementType.WATER)
            updateButtonSelection(ElementType.WATER)
        }

        binding.resetButton.setOnClickListener {
            binding.gardenView.resetGame()
        }

        // Начальный выбор
        updateButtonSelection(ElementType.STONE)
    }

    private fun updateButtonSelection(selectedType: ElementType) {
        binding.stoneButton.isSelected = selectedType == ElementType.STONE
        binding.flowerButton.isSelected = selectedType == ElementType.FLOWER
        binding.leafButton.isSelected = selectedType == ElementType.LEAF
        binding.waterButton.isSelected = selectedType == ElementType.WATER
    }

    override fun onScoreChanged(score: Int) {
        binding.scoreText.text = getString(R.string.score, score)
    }

    override fun onPatternCreated(patternType: String, score: Int) {
        // Можно добавить визуальную обратную связь
        binding.comboText.text = getString(R.string.combo, binding.gardenView.getCombo())
        binding.comboText.alpha = 1f
        binding.comboText.animate().alpha(0f).setDuration(2000).start()
    }
}
