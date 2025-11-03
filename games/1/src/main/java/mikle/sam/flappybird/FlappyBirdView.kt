package mikle.sam.flappybird

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class FlappyBirdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bird: Bird? = null
    private var pipes = mutableListOf<Pipe>()
    private var gameStarted = false
    private var gameOver = false
    private var score = 0
    private var bestScore = 0
    
    private val paint = Paint().apply {
        color = 0xFFFFFFFF.toInt() // White background
        isAntiAlias = true
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val gameLoop = object : Runnable {
        override fun run() {
            update()
            invalidate()
            if (!gameOver) {
                handler.postDelayed(this, 16) // ~60 FPS
            }
        }
    }
    
    private var onGameStateChangeListener: OnGameStateChangeListener? = null
    
    interface OnGameStateChangeListener {
        fun onScoreChanged(score: Int)
        fun onGameOver(score: Int, bestScore: Int)
        fun onGameStarted()
        fun onGameRestarted()
    }
    
    fun setOnGameStateChangeListener(listener: OnGameStateChangeListener) {
        this.onGameStateChangeListener = listener
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetGame()
    }
    
    private fun resetGame() {
        bird = Bird(
            x = width * 0.2f,
            y = height * 0.5f
        )
        pipes.clear()
        gameStarted = false
        gameOver = false
        score = 0
        onGameStateChangeListener?.onScoreChanged(score)
        onGameStateChangeListener?.onGameRestarted()
    }
    
    fun startGame() {
        if (!gameStarted && !gameOver) {
            gameStarted = true
            onGameStateChangeListener?.onGameStarted()
            handler.post(gameLoop)
        }
    }
    
    fun restartGame() {
        handler.removeCallbacks(gameLoop)
        resetGame()
    }
    
    private fun update() {
        if (!gameStarted || gameOver) return
        
        // Update bird
        bird?.update()
        
        // Check if bird hits ground or ceiling
        if (bird?.getY() ?: 0f > height - 30f || bird?.getY() ?: 0f < 30f) {
            gameOver()
            return
        }
        
        // Update pipes
        pipes.forEach { it.update() }
        
        // Remove off-screen pipes
        pipes.removeAll { it.isOffScreen() }
        
        // Add new pipes
        if (pipes.isEmpty() || pipes.last().getX() < width - 500f) {
            pipes.add(Pipe(
                x = width.toFloat(),
                gapSize = 500f,  // Increased gap for easier gameplay
                screenHeight = height.toFloat()
            ))
        }
        
        // Check collisions
        bird?.let { bird ->
            val birdBounds = bird.getBounds()
            pipes.forEach { pipe ->
                pipe.getBounds().forEach { pipeBounds ->
                    if (RectF.intersects(birdBounds, pipeBounds)) {
                        gameOver()
                        return
                    }
                }
            }
        }
        
        // Check scoring
        bird?.let { bird ->
            pipes.forEach { pipe ->
                if (!pipe.hasScored() && bird.getX() > pipe.getX() + 80f) {
                    pipe.markScored()
                    score++
                    onGameStateChangeListener?.onScoreChanged(score)
                }
            }
        }
    }
    
    private fun gameOver() {
        gameOver = true
        gameStarted = false
        if (score > bestScore) {
            bestScore = score
        }
        onGameStateChangeListener?.onGameOver(score, bestScore)
        handler.removeCallbacks(gameLoop)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawColor(0xFFFFFFFF.toInt())
        
        // Draw bird
        bird?.draw(canvas)
        
        // Draw pipes
        pipes.forEach { it.draw(canvas) }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!gameStarted && !gameOver) {
                startGame()
            } else if (gameStarted && !gameOver) {
                bird?.jump()
            } else if (gameOver) {
                restartGame()
            }
            return true
        }
        return super.onTouchEvent(event)
    }
    
    fun getScore(): Int = score
    fun getBestScore(): Int = bestScore
    fun isGameOver(): Boolean = gameOver
    fun isGameStarted(): Boolean = gameStarted
}
