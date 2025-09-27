package mikle.sam.game0

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.roundToInt

class Player(private val animationFrames: List<Bitmap>, private val screenHeight: Int, private var lanePositions: List<Float>) {
    val rect: RectF
    private var currentLane = lanePositions.size / 2
    private var currentFrameIndex = 0
    private var lastFrameChangeTime = 0L
    private val frameDuration = 100L // 100ms

    init {
        val playerWidth = 200f
        val playerHeight = 200f
        val initialX = lanePositions[currentLane]
        val initialY = screenHeight * 0.75f
        rect = RectF(initialX - playerWidth / 2, initialY - playerHeight / 2, initialX + playerWidth / 2, initialY + playerHeight / 2)
        lastFrameChangeTime = System.currentTimeMillis()
    }

    fun update() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameChangeTime > frameDuration) {
            currentFrameIndex = (currentFrameIndex + 1) % animationFrames.size
            lastFrameChangeTime = currentTime
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
        if (animationFrames.isNotEmpty()) {
            canvas.drawBitmap(animationFrames[currentFrameIndex], null, rect, null)
        }
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