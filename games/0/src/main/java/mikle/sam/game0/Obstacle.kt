package mikle.sam.game0

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

enum class ObstacleType {
    SQUARE, CIRCLE
}

class Obstacle(x: Float, y: Float, val type: ObstacleType, private val animationFrames: List<Bitmap>? = null, speedBoost: Float = 0f) {
    val rect: RectF
    private val paint: Paint = Paint()
    private var currentFrameIndex = 0
    private var lastFrameChangeTime = 0L
    private val frameDuration = 150L // 150ms
    private var speed: Float
    private val originalSpeed: Float

    init {
        rect = RectF(x - 100f, y - 100f, x + 100f, y + 100f)
        when (type) {
            ObstacleType.SQUARE -> {
                paint.color = Color.WHITE
                speed = (80..85).random().toFloat()/10 + speedBoost
            }
            ObstacleType.CIRCLE -> {
                paint.color = Color.WHITE
                speed = (10..11).random().toFloat()
            }
        }
        originalSpeed = speed
        lastFrameChangeTime = System.currentTimeMillis()
    }

    fun update() {
        rect.top += speed
        rect.bottom += speed

        if (type == ObstacleType.SQUARE && animationFrames != null) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFrameChangeTime > frameDuration) {
                currentFrameIndex = (currentFrameIndex + 1) % animationFrames.size
                lastFrameChangeTime = currentTime
            }
        }
    }

    fun slowDown() {
        speed = maxOf(1f, originalSpeed - 7f)
    }

    fun speedUp() {
        speed = originalSpeed + 5f
    }

    fun restoreSpeed() {
        speed = originalSpeed
    }

    fun draw(canvas: Canvas) {
        when (type) {
            ObstacleType.SQUARE -> {
                if (animationFrames != null && animationFrames.isNotEmpty()) {
                    canvas.drawBitmap(animationFrames[currentFrameIndex], null, rect, null)
                } else {
                    canvas.drawRect(rect, paint)
                }
            }
            ObstacleType.CIRCLE -> canvas.drawOval(rect, paint)
        }
    }
}