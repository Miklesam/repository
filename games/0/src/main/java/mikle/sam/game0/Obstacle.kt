package mikle.sam.game0

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.sin

enum class ObstacleType {
    SQUARE, CIRCLE
}

class Obstacle(x: Float, y: Float, val type: ObstacleType, speedBoost: Float = 0f) {
    val rect: RectF
    private val bodyPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val limbPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }
    private var lastAnimationUpdate = 0L
    private var walkAnimationPhase = 0f
    private var speed: Float
    private val originalSpeed: Float

    init {
        rect = RectF(x - 100f, y - 100f, x + 100f, y + 100f)
        when (type) {
            ObstacleType.SQUARE -> {
                speed = (80..85).random().toFloat()/10 + speedBoost
            }
            ObstacleType.CIRCLE -> {
                speed = (10..11).random().toFloat()
            }
        }
        originalSpeed = speed
        lastAnimationUpdate = System.currentTimeMillis()
    }

    fun update() {
        rect.top += speed
        rect.bottom += speed

        // Animate walking
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnimationUpdate > 70) {
            walkAnimationPhase = (walkAnimationPhase + 0.45f) % (PI * 2f).toFloat()
            lastAnimationUpdate = currentTime
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
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val width = rect.width()
        val height = rect.height()
        
        // Calculate walking animation offsets (for view from behind, moving upward)
        // Smaller animation amplitude for obstacles
        val swingAmount = sin(walkAnimationPhase) * 15f  // Less than Player (25f)
        val leftArmForward = swingAmount
        val rightArmForward = -swingAmount
        
        val leftLegForward = -swingAmount * 0.5f  // Less than Player (0.7f)
        val rightLegForward = swingAmount * 0.5f
        
        // Head (viewed from behind - at top)
        val headRadius = width * 0.22f
        val headY = rect.top + headRadius + 15f
        canvas.drawCircle(centerX, headY, headRadius, bodyPaint)
        
        // Add hair detail (view from behind)
        val hairPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        // Draw hair lines
        canvas.drawArc(
            centerX - headRadius * 0.8f,
            headY - headRadius * 0.8f,
            centerX + headRadius * 0.8f,
            headY + headRadius * 0.8f,
            0f, 180f, false, hairPaint
        )
        
        // Body/back (trapezoid shape - wider at shoulders, narrower at waist)
        val bodyTop = headY + headRadius + 5f
        val bodyBottom = bodyTop + height * 0.4f
        val shoulderWidth = width * 0.5f
        val waistWidth = width * 0.35f
        
        val bodyPath = Path().apply {
            moveTo(centerX - shoulderWidth / 2, bodyTop)
            lineTo(centerX + shoulderWidth / 2, bodyTop)
            lineTo(centerX + waistWidth / 2, bodyBottom)
            lineTo(centerX - waistWidth / 2, bodyBottom)
            close()
        }
        canvas.drawPath(bodyPath, bodyPaint)
        
        // Shoulder positions (at top of body)
        val shoulderY = bodyTop
        val shoulderSpread = shoulderWidth * 0.4f
        
        // Left arm (swinging forward/backward when viewed from behind, moving upward)
        val leftShoulderX = centerX - shoulderSpread
        val leftElbowX = leftShoulderX
        val leftElbowY = shoulderY + height * 0.15f + leftArmForward * 0.3f
        val leftHandX = leftElbowX
        val leftHandY = leftElbowY + height * 0.2f + leftArmForward * 0.5f
        canvas.drawLine(leftShoulderX, shoulderY, leftElbowX, leftElbowY, limbPaint)
        canvas.drawLine(leftElbowX, leftElbowY, leftHandX, leftHandY, limbPaint)
        
        // Right arm (swinging opposite)
        val rightShoulderX = centerX + shoulderSpread
        val rightElbowX = rightShoulderX
        val rightElbowY = shoulderY + height * 0.15f + rightArmForward * 0.3f
        val rightHandX = rightElbowX
        val rightHandY = rightElbowY + height * 0.2f + rightArmForward * 0.5f
        canvas.drawLine(rightShoulderX, shoulderY, rightElbowX, rightElbowY, limbPaint)
        canvas.drawLine(rightElbowX, rightElbowY, rightHandX, rightHandY, limbPaint)
        
        // Hip position (at bottom of body)
        val hipY = bodyBottom
        val hipSpread = waistWidth * 0.3f
        
        // Left leg (viewed from behind, moving forward/backward when walking upward)
        val leftHipX = centerX - hipSpread
        val leftKneeX = leftHipX
        val leftKneeY = hipY + height * 0.15f + leftLegForward * 0.4f
        val leftFootX = leftKneeX
        val leftFootY = rect.bottom - 10f + leftLegForward * 0.6f
        canvas.drawLine(leftHipX, hipY, leftKneeX, leftKneeY, limbPaint)
        canvas.drawLine(leftKneeX, leftKneeY, leftFootX, leftFootY, limbPaint)
        
        // Right leg (swinging opposite)
        val rightHipX = centerX + hipSpread
        val rightKneeX = rightHipX
        val rightKneeY = hipY + height * 0.15f + rightLegForward * 0.4f
        val rightFootX = rightKneeX
        val rightFootY = rect.bottom - 10f + rightLegForward * 0.6f
        canvas.drawLine(rightHipX, hipY, rightKneeX, rightKneeY, limbPaint)
        canvas.drawLine(rightKneeX, rightKneeY, rightFootX, rightFootY, limbPaint)
    }
}