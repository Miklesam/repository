package mikle.sam.flappybird

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Pipe(
    private var x: Float,
    private val gapSize: Float,
    private val screenHeight: Float,
    private val pipeWidth: Float = 80f
) {
    private val pipeSpeed = 5f
    private val paint = Paint().apply {
        color = 0xFF2E8B57.toInt() // Pipe green color
        isAntiAlias = true
    }
    
    // Random gap position
    private val gapY = (screenHeight * 0.2f + Math.random() * screenHeight * 0.6f).toFloat()
    private val topPipeHeight = gapY - gapSize / 2f
    private val bottomPipeY = gapY + gapSize / 2f
    private val bottomPipeHeight = screenHeight - bottomPipeY
    
    private var hasScored = false
    
    fun update() {
        x -= pipeSpeed
    }
    
    fun draw(canvas: Canvas) {
        // Draw top pipe
        val topPipe = RectF(x, 0f, x + pipeWidth, topPipeHeight)
        canvas.drawRect(topPipe, paint)
        
        // Draw bottom pipe
        val bottomPipe = RectF(x, bottomPipeY, x + pipeWidth, screenHeight)
        canvas.drawRect(bottomPipe, paint)
    }
    
    fun isOffScreen(): Boolean {
        return x + pipeWidth < 0
    }
    
    fun getBounds(): List<RectF> {
        return listOf(
            RectF(x, 0f, x + pipeWidth, topPipeHeight), // Top pipe
            RectF(x, bottomPipeY, x + pipeWidth, screenHeight) // Bottom pipe
        )
    }
    
    fun getGapBounds(): RectF {
        return RectF(x, gapY - gapSize / 2f, x + pipeWidth, gapY + gapSize / 2f)
    }
    
    fun getX(): Float = x
    fun hasScored(): Boolean = hasScored
    fun markScored() { hasScored = true }
}
