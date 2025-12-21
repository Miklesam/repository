package mikle.sam.game0

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class Player(private val screenHeight: Int, private var lanePositions: List<Float>) {
    val rect: RectF
    private var currentLane = lanePositions.size / 2
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
    private var walkAnimationPhase = 0f
    private var lastAnimationUpdate = System.currentTimeMillis()

    init {
        val playerWidth = 200f
        val playerHeight = 200f
        val initialX = lanePositions[currentLane]
        val initialY = screenHeight * 0.75f
        rect = RectF(initialX - playerWidth / 2, initialY - playerHeight / 2, initialX + playerWidth / 2, initialY + playerHeight / 2)
    }

    fun update() {
        // Animate walking
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnimationUpdate > 70) {
            walkAnimationPhase = (walkAnimationPhase + 0.45f) % (PI * 2f).toFloat()
            lastAnimationUpdate = currentTime
        }
    }

    fun moveLeft() {
        if (currentLane > 0) {
            currentLane--
            updatePosition()
        }
    }

    fun moveRight() {
        if (currentLane < lanePositions.size - 1) {
            currentLane++
            updatePosition()
        }
    }

    private fun updatePosition() {
        val newX = lanePositions[currentLane]
        rect.left = newX - 100f
        rect.right = newX + 100f
    }

    fun draw(canvas: Canvas) {
        var centerX = rect.centerX()
        val centerY = rect.centerY()
        val width = rect.width()
        val height = rect.height()
        
        // Calculate walking animation offsets (for view from behind, moving upward)
        // When walking upward, arms and legs swing forward/backward (up/down)
        // Left arm and right leg move forward together, right arm and left leg move backward together
        val swingAmount = sin(walkAnimationPhase) * 25f
        val leftArmForward = swingAmount  // Left arm moves forward (up) when positive
        val rightArmForward = -swingAmount  // Right arm moves backward (down) when positive
        
        val leftLegForward = -swingAmount * 0.7f  // Left leg moves backward (down) when positive
        val rightLegForward = swingAmount * 0.7f  // Right leg moves forward (up) when positive
        
        
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

    fun reset() {
        currentLane = lanePositions.size / 2
        updatePosition()
    }

    fun updateLanePositions(newLanes: List<Float>) {
        val oldLaneCount = lanePositions.size
        val newLaneCount = newLanes.size

        if (oldLaneCount != newLaneCount && oldLaneCount > 1) {
            val relativePosition = currentLane.toFloat() / (oldLaneCount - 1)
            currentLane = (relativePosition * (newLaneCount - 1)).roundToInt()
        } else if (oldLaneCount != newLaneCount) {
            currentLane = newLaneCount / 2
        }
        currentLane = currentLane.coerceIn(0, newLaneCount - 1)

        lanePositions = newLanes
        if (currentLane < lanePositions.size) {
            updatePosition()
        }
    }

}