package mikle.sam.game0

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.sin

enum class ObstacleType {
    NORMAL,      // Обычное препятствие
    FAST,        // Быстрое препятствие
    SLOW,        // Медленное препятствие (больше очков)
    BIG,         // Большое препятствие
    SMALL,       // Маленькое препятствие
    LANE_CHANGER // Меняет полосы
}

class Obstacle(x: Float, y: Float, val type: ObstacleType, speedBoost: Float = 0f, lanePositions: List<Float>? = null) {
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
        strokeCap = Paint.Cap.ROUND
    }
    private val hairPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private val indicatorPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private val arrowPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private val bodyPath = Path()
    private var lastAnimationUpdate = 0L
    private var walkAnimationPhase = 0f
    private var speed: Float
    private val originalSpeed: Float
    private val sizeMultiplier: Float
    private var lanePositions: List<Float>? = lanePositions
    private var currentLaneIndex: Int = 0
    private var lastLaneChangeTime = 0L
    private val laneChangeInterval = 2000L // Меняет полосу каждые 2 секунды

    init {
        // Определяем размер в зависимости от типа
        sizeMultiplier = when (type) {
            ObstacleType.BIG -> 1.5f
            ObstacleType.SMALL -> 0.7f
            else -> 1.0f
        }

        val baseSize = 100f * sizeMultiplier
        rect = RectF(x - baseSize, y - baseSize, x + baseSize, y + baseSize)

        // Определяем скорость в зависимости от типа
        val baseSpeed = when (type) {
            ObstacleType.FAST -> (12..14).random().toFloat()
            ObstacleType.SLOW -> (6..8).random().toFloat()
            ObstacleType.BIG -> (7..9).random().toFloat()
            ObstacleType.SMALL -> (10..12).random().toFloat()
            ObstacleType.LANE_CHANGER -> (8..10).random().toFloat()
            ObstacleType.NORMAL -> (8..8).random().toFloat()
        }

        speed = baseSpeed + speedBoost
        originalSpeed = speed
        lastAnimationUpdate = System.currentTimeMillis()
        lastLaneChangeTime = System.currentTimeMillis()

        // Обновляем strokeWidth для Paint объектов в зависимости от размера
        limbPaint.strokeWidth = 20f * sizeMultiplier
        hairPaint.strokeWidth = 3f * sizeMultiplier

        // Находим начальную полосу для LANE_CHANGER
        if (type == ObstacleType.LANE_CHANGER && lanePositions != null) {
            currentLaneIndex = lanePositions.indexOfFirst {
                kotlin.math.abs(it - x) < 10f
            }.takeIf { it >= 0 } ?: 0
        }
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

        // Логика смены полос для LANE_CHANGER
        if (type == ObstacleType.LANE_CHANGER && lanePositions != null && lanePositions!!.size > 1) {
            if (currentTime - lastLaneChangeTime > laneChangeInterval) {
                // Меняем полосу случайным образом
                val newLane = (0 until lanePositions!!.size).random()
                if (newLane != currentLaneIndex) {
                    currentLaneIndex = newLane
                    val newX = lanePositions!![currentLaneIndex]
                    val centerX = rect.centerX()
                    val width = rect.width()
                    rect.left = newX - width / 2
                    rect.right = newX + width / 2
                    lastLaneChangeTime = currentTime
                }
            }
        }
    }

    fun updateLanePositions(newLanePositions: List<Float>) {
        if (type == ObstacleType.LANE_CHANGER) {
            lanePositions = newLanePositions
            // Если количество полос изменилось, выбираем ближайшую полосу
            if (newLanePositions.isNotEmpty()) {
                if (currentLaneIndex >= newLanePositions.size) {
                    currentLaneIndex = newLanePositions.size - 1
                }
                val newX = newLanePositions[currentLaneIndex]
                val width = rect.width()
                rect.left = newX - width / 2
                rect.right = newX + width / 2
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
        // Draw hair lines
        canvas.drawArc(
            centerX - headRadius * 0.8f,
            headY - headRadius * 0.8f,
            centerX + headRadius * 0.8f,
            headY + headRadius * 0.8f,
            0f, 180f, false, hairPaint
        )
        
        // Визуальные индикаторы для разных типов препятствий
        when (type) {
            ObstacleType.FAST -> {
                // Маленькие линии по бокам для быстрых
                indicatorPaint.strokeWidth = 2f * sizeMultiplier
                canvas.drawLine(centerX - width * 0.4f, headY, centerX - width * 0.3f, headY, indicatorPaint)
                canvas.drawLine(centerX + width * 0.3f, headY, centerX + width * 0.4f, headY, indicatorPaint)
            }
            ObstacleType.SLOW -> {
                // Горизонтальная линия для медленных
                indicatorPaint.strokeWidth = 3f * sizeMultiplier
                canvas.drawLine(centerX - width * 0.2f, headY - headRadius * 0.5f,
                               centerX + width * 0.2f, headY - headRadius * 0.5f, indicatorPaint)
            }
            ObstacleType.LANE_CHANGER -> {
                // Стрелки по бокам для меняющих полосы
                arrowPaint.strokeWidth = 2f * sizeMultiplier
                val arrowSize = width * 0.15f
                // Левая стрелка
                canvas.drawLine(centerX - width * 0.35f, headY, centerX - width * 0.45f, headY - arrowSize, arrowPaint)
                canvas.drawLine(centerX - width * 0.35f, headY, centerX - width * 0.45f, headY + arrowSize, arrowPaint)
                // Правая стрелка
                canvas.drawLine(centerX + width * 0.35f, headY, centerX + width * 0.45f, headY - arrowSize, arrowPaint)
                canvas.drawLine(centerX + width * 0.35f, headY, centerX + width * 0.45f, headY + arrowSize, arrowPaint)
            }
            else -> {
                // Для остальных типов без дополнительных индикаторов
            }
        }

        // Body/back (trapezoid shape - wider at shoulders, narrower at waist)
        val bodyTop = headY + headRadius + 5f
        val bodyBottom = bodyTop + height * 0.4f
        val shoulderWidth = width * 0.5f
        val waistWidth = width * 0.35f
        
        bodyPath.reset()
        bodyPath.moveTo(centerX - shoulderWidth / 2, bodyTop)
        bodyPath.lineTo(centerX + shoulderWidth / 2, bodyTop)
        bodyPath.lineTo(centerX + waistWidth / 2, bodyBottom)
        bodyPath.lineTo(centerX - waistWidth / 2, bodyBottom)
        bodyPath.close()
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