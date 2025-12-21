package mikle.sam.game0

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.os.Bundle
import android.view.View

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val laneCount = intent.getIntExtra("laneCount", 2)
        val frequency = intent.getIntExtra("frequency", 101-33)
        gameView = GameView(this, null)
        gameView.setGameParameters(laneCount, frequency)
        gameView.setTimedGameMode()
        setContentView(gameView)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.navigationBarColor = Color.WHITE
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
}