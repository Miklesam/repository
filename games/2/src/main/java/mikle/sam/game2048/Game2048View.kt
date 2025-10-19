package mikle.sam.game2048

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class Game2048View @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var game: Game2048
    private var tileSize = 0f
    private var margin = 0f
    private val gridPaint = Paint().apply {
        color = 0xFFBBADA0.toInt()
        isAntiAlias = true
    }
    
    private var onGameStateChangeListener: OnGameStateChangeListener? = null
    
    interface OnGameStateChangeListener {
        fun onScoreChanged(score: Int)
        fun onGameWon()
        fun onGameOver()
        fun onGameReset()
    }
    
    fun setOnGameStateChangeListener(listener: OnGameStateChangeListener) {
        this.onGameStateChangeListener = listener
    }
    
    private var startX = 0f
    private var startY = 0f
    private val minSwipeDistance = 10f
    
    private val handler = Handler(Looper.getMainLooper())
    private val animationLoop = object : Runnable {
        override fun run() {
            updateAnimations()
            invalidate()
            handler.postDelayed(this, 16) // ~60 FPS
        }
    }
    private var isAnimating = false
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val size = minOf(w, h) - 40f // 20dp margin on each side
        tileSize = size / 4f
        margin = 8f
        
        game = Game2048()
        onGameStateChangeListener?.onScoreChanged(game.getScore())
    }
    
    private fun updateAnimations() {
        var anyAnimating = false
        
        for (row in game.getGrid()) {
            for (tile in row) {
                if (tile.isAnimating) {
                    tile.updateAnimation(0.016f) // ~60 FPS
                    anyAnimating = true
                }
            }
        }
        
        if (!anyAnimating && isAnimating) {
            isAnimating = false
            handler.removeCallbacks(animationLoop)
        }
    }
    
    private fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            handler.post(animationLoop)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        drawBackground(canvas)
        drawGrid(canvas)
        drawTiles(canvas)
    }
    
    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(0xFFFAF8EF.toInt())
    }
    
    private fun drawGrid(canvas: Canvas) {
        val gridSize = tileSize * 4 + margin * 5
        val startX = (width - gridSize) / 2f
        val startY = (height - gridSize) / 2f
        
        // Draw grid background
        val gridRect = android.graphics.RectF(
            startX,
            startY,
            startX + gridSize,
            startY + gridSize
        )
        canvas.drawRoundRect(gridRect, 6f, 6f, gridPaint)
        
        // Draw grid lines
        val linePaint = Paint().apply {
            color = 0xFFBBADA0.toInt()
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        for (i in 0..4) {
            val x = startX + i * (tileSize + margin)
            canvas.drawLine(x, startY, x, startY + gridSize, linePaint)
            
            val y = startY + i * (tileSize + margin)
            canvas.drawLine(startX, y, startX + gridSize, y, linePaint)
        }
    }
    
    private fun drawTiles(canvas: Canvas) {
        val gridSize = tileSize * 4 + margin * 5
        val startX = (width - gridSize) / 2f
        val startY = (height - gridSize) / 2f
        
        android.util.Log.d("Game2048", "Drawing tiles - gridSize: $gridSize, startX: $startX, startY: $startY")
        
        for (rowIndex in 0 until 4) {
            for (colIndex in 0 until 4) {
                val tile = game.getGrid()[rowIndex][colIndex]
                android.util.Log.d("Game2048", "Tile at [$rowIndex][$colIndex]: value=${tile.value}")
                if (tile.value != 0) {
                    tile.draw(canvas, tileSize, margin, startX, startY)
                }
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                android.util.Log.d("Game2048", "Touch down at: $startX, $startY")
                return true
            }
            MotionEvent.ACTION_UP -> {
                val endX = event.x
                val endY = event.y
                
                val deltaX = endX - startX
                val deltaY = endY - startY
                
                if (abs(deltaX) > minSwipeDistance || abs(deltaY) > minSwipeDistance) {
                    val direction = when {
                        abs(deltaX) > abs(deltaY) -> {
                            if (deltaX > 0) Direction.RIGHT else Direction.LEFT
                        }
                        else -> {
                            if (deltaY > 0) Direction.DOWN else Direction.UP
                        }
                    }
                    
                    android.util.Log.d("Game2048", "Swipe detected: $direction, deltaX: $deltaX, deltaY: $deltaY")
                    val moved = game.move(direction)
                    android.util.Log.d("Game2048", "Move result: $moved")
                    
                    if (moved) {
                        startAnimation()
                        onGameStateChangeListener?.onScoreChanged(game.getScore())
                        
                        if (game.isWon() && !game.isOver()) {
                            onGameStateChangeListener?.onGameWon()
                        }
                        
                        if (game.isOver()) {
                            onGameStateChangeListener?.onGameOver()
                        }
                        
                        invalidate()
                    }
                }
                return true
            }
        }
        return true
    }
    
    fun resetGame() {
        game.reset()
        onGameStateChangeListener?.onScoreChanged(game.getScore())
        onGameStateChangeListener?.onGameReset()
        invalidate()
    }
    
    fun getScore(): Int = game.getScore()
    fun isGameOver(): Boolean = game.isOver()
    fun isGameWon(): Boolean = game.isWon()
}
