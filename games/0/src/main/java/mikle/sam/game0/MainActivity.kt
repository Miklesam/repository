package mikle.sam.game0

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import mikle.sam.game0.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameButton.setOnClickListener { startGame(0) }
        binding.twoLanesButton.setOnClickListener { startGame(2) }
        binding.threeLanesButton.setOnClickListener { startGame(3) }
        binding.fourLanesButton.setOnClickListener { startGame(4) }

        binding.frequencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.frequencyValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startGame(laneCount: Int) {
        val progress = binding.frequencySeekBar.progress
        val frequency = 101 - progress // Invert progress to frequency
        val intent = Intent(this, GameActivity::class.java)
        if (laneCount == 0) {
            intent.putExtra("gameMode", "timed")
            intent.putExtra("laneCount", 2)
        } else {
            intent.putExtra("laneCount", laneCount)
        }
        intent.putExtra("frequency", frequency)
        startActivity(intent)
    }
}