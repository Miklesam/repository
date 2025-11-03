package mikle.sam.flappybird

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Bird(
    private var x: Float,
    private var y: Float,
    private val radius: Float = 30f
) {
    private var velocityY = 0f
    private val gravity = 0.8f
    private val jumpVelocity = -15f
    private val paint = Paint().apply {
        color = 0xFF000000.toInt() // Black bird
        isAntiAlias = true
    }
    
    fun update() {
        velocityY += gravity
        y += velocityY
    }
    
    fun jump() {
        velocityY = jumpVelocity
    }
    
    fun draw(canvas: Canvas) {
        // Draw bird body (circle)
        canvas.drawCircle(x, y, radius, paint)
        
        // Draw bird eye (white on black bird)
        val eyePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(x + 10f, y - 10f, 8f, eyePaint)
        
        // Draw bird pupil (black on white eye)
        val pupilPaint = Paint().apply {
            color = 0xFF000000.toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(x + 12f, y - 8f, 4f, pupilPaint)
    }
    
    fun getBounds(): RectF {
        return RectF(x - radius, y - radius, x + radius, y + radius)
    }
    
    fun getY(): Float = y
    fun getX(): Float = x
    fun setY(newY: Float) { y = newY }
}
