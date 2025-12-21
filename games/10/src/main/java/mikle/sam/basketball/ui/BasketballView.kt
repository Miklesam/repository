package mikle.sam.basketball.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class BasketballView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onScoreChanged(score: Int)
        fun onShotsChanged(shots: Int, made: Int)
        fun onTimeChanged(timeLeft: Int)
        fun onGameOver(finalScore: Int, shotsMade: Int, totalShots: Int)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballPath = Path()
    private val netPath = Path()

    private val density = resources.displayMetrics.density

    private var listener: Listener? = null

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var lastFrameTime = 0L
    private var running = false
    private var gameHasStarted = false

    private var score = 0
    private var shotsMade = 0
    private var totalShots = 0
    private var timeLeft = GAME_TIME_SECONDS
    private var gameStartTime = 0L

    private lateinit var ball: Ball
    private lateinit var hoop: Hoop
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var ballInFlight = false

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            updateGame()
            invalidate()
            postDelayed(this, FRAME_DELAY_MS)
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun startGame() {
        score = 0
        shotsMade = 0
        totalShots = 0
        timeLeft = GAME_TIME_SECONDS
        listener?.onScoreChanged(score)
        listener?.onShotsChanged(shotsMade, totalShots)
        listener?.onTimeChanged(timeLeft)

        setupBall()
        setupHoop()

        gameHasStarted = true
        running = true
        val now = SystemClock.elapsedRealtime()
        gameStartTime = now
        lastFrameTime = now

        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun pauseGame() {
        if (!running) return
        running = false
        removeCallbacks(frameRunnable)
    }

    fun resumeGame() {
        if (!gameHasStarted || running) return
        running = true
        lastFrameTime = SystemClock.elapsedRealtime()
        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun stopGame() {
        running = false
        gameHasStarted = false
        removeCallbacks(frameRunnable)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        setupBall()
        setupHoop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем фон (пол)
        drawBackground(canvas)

        // Рисуем корзину
        drawHoop(canvas)

        // Рисуем линию натяжения
        if (isDragging && !ballInFlight) {
            drawPowerLine(canvas)
        }

        // Рисуем мяч
        drawBall(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!ballInFlight && !isDragging) {
                    isDragging = true
                    dragStartX = event.x
                    dragStartY = event.y
                    dragCurrentX = event.x
                    dragCurrentY = event.y
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !ballInFlight) {
                    dragCurrentX = event.x
                    dragCurrentY = event.y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isDragging && !ballInFlight) {
                    shootBall()
                    isDragging = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
    }

    private fun updateGame() {
        val now = SystemClock.elapsedRealtime()
        val deltaMillis = (now - lastFrameTime).coerceAtMost(MAX_FRAME_DELTA)
        lastFrameTime = now
        val deltaSeconds = deltaMillis / 1000f

        // Обновляем таймер
        if (gameHasStarted) {
            val elapsed = (now - gameStartTime) / 1000
            val newTimeLeft = GAME_TIME_SECONDS - elapsed.toInt()
            if (newTimeLeft != timeLeft) {
                timeLeft = newTimeLeft
                listener?.onTimeChanged(timeLeft)
                if (timeLeft <= 0) {
                    finishGame()
                    return
                }
            }
        }

        // Обновляем мяч в полете
        if (ballInFlight) {
            updateBall(deltaSeconds)
            checkBasketCollision()
        }
    }

    private fun updateBall(deltaSeconds: Float) {
        // Применяем гравитацию
        ball.velocityY += GRAVITY * deltaSeconds

        // Обновляем позицию
        ball.x += ball.velocityX * deltaSeconds
        ball.y += ball.velocityY * deltaSeconds

        // Вращение мяча
        ball.rotation += ball.rotationSpeed * deltaSeconds

        // Проверяем выход за границы
        if (ball.y > viewHeight + ball.radius * 2) {
            resetBall()
        }
    }

    private fun checkBasketCollision() {
        val dx = ball.x - hoop.centerX
        val dy = ball.y - hoop.centerY
        val distance = sqrt(dx * dx + dy * dy)

        // Проверяем попадание в корзину
        if (distance < hoop.radius && ball.y > hoop.centerY && ball.velocityY > 0) {
            // Мяч попал в корзину
            score += BASKET_SCORE
            shotsMade++
            totalShots++
            listener?.onScoreChanged(score)
            listener?.onShotsChanged(shotsMade, totalShots)
            resetBall()
        }
    }

    private fun shootBall() {
        if (ballInFlight) return

        val dx = dragCurrentX - dragStartX
        val dy = dragCurrentY - dragStartY
        val distance = sqrt(dx * dx + dy * dy)

        // Ограничиваем максимальную силу
        val maxDistance = viewHeight * 0.4f
        val normalizedDistance = min(distance / maxDistance, 1f)

        // Вычисляем угол и скорость
        val angle = atan2(-dy, dx)
        val power = normalizedDistance * MAX_SHOOT_POWER

        ball.velocityX = cos(angle) * power
        ball.velocityY = sin(angle) * power
        ball.rotationSpeed = (ball.velocityX / ball.radius) * 180f / kotlin.math.PI.toFloat()

        ballInFlight = true
        totalShots++
        listener?.onShotsChanged(shotsMade, totalShots)
    }

    private fun resetBall() {
        ballInFlight = false
        setupBall()
    }

    private fun drawBackground(canvas: Canvas) {
        // Пол
        paint.color = Color.parseColor("#2D5016")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, viewHeight * 0.7f, viewWidth, viewHeight, paint)

        // Линии на полу
        paint.color = Color.parseColor("#FFFFFF")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        val lineY = viewHeight * 0.7f
        canvas.drawLine(0f, lineY, viewWidth, lineY, paint)

        // Центральная линия
        val centerLineY = viewHeight * 0.85f
        canvas.drawLine(viewWidth / 2f - 50f, centerLineY, viewWidth / 2f + 50f, centerLineY, paint)
        canvas.drawCircle(viewWidth / 2f, centerLineY, 30f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawHoop(canvas: Canvas) {
        // Щит
        val backboardWidth = 120f
        val backboardHeight = 80f
        val backboardX = hoop.centerX - backboardWidth / 2f
        val backboardY = hoop.centerY - hoop.radius - backboardHeight

        paint.color = Color.parseColor("#FFFFFF")
        paint.style = Paint.Style.FILL
        canvas.drawRect(backboardX, backboardY, backboardX + backboardWidth, backboardY + backboardHeight, paint)

        paint.color = Color.parseColor("#000000")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(backboardX, backboardY, backboardX + backboardWidth, backboardY + backboardHeight, paint)

        // Обод корзины
        paint.color = Color.parseColor("#FF6B35")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawCircle(hoop.centerX, hoop.centerY, hoop.radius, paint)

        // Сетка
        drawNet(canvas)
    }

    private fun drawNet(canvas: Canvas) {
        paint.color = Color.parseColor("#C0C0C0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        val netSegments = 8
        val netDepth = 40f
        val segmentAngle = 360f / netSegments

        for (i in 0 until netSegments) {
            val angle1 = (i * segmentAngle) * kotlin.math.PI / 180f
            val angle2 = ((i + 1) * segmentAngle) * kotlin.math.PI / 180f

            val x1 = hoop.centerX + cos(angle1).toFloat() * hoop.radius
            val y1 = hoop.centerY + sin(angle1).toFloat() * hoop.radius
            val x2 = hoop.centerX + cos(angle2).toFloat() * hoop.radius
            val y2 = hoop.centerY + sin(angle2).toFloat() * hoop.radius

            val x3 = hoop.centerX + cos(angle1).toFloat() * (hoop.radius - 5f)
            val y3 = hoop.centerY + sin(angle1).toFloat() * (hoop.radius - 5f) + netDepth
            val x4 = hoop.centerX + cos(angle2).toFloat() * (hoop.radius - 5f)
            val y4 = hoop.centerY + sin(angle2).toFloat() * (hoop.radius - 5f) + netDepth

            canvas.drawLine(x1, y1, x3, y3, paint)
            canvas.drawLine(x2, y2, x4, y4, paint)
            canvas.drawLine(x3, y3, x4, y4, paint)
        }
    }

    private fun drawPowerLine(canvas: Canvas) {
        val dx = dragCurrentX - dragStartX
        val dy = dragCurrentY - dragStartY
        val distance = sqrt(dx * dx + dy * dy)
        val maxDistance = viewHeight * 0.4f
        val normalizedDistance = min(distance / maxDistance, 1f)

        // Цвет линии в зависимости от силы
        val red = (normalizedDistance * 255).toInt().coerceIn(0, 255)
        val green = ((1f - normalizedDistance) * 255).toInt().coerceIn(0, 255)
        paint.color = Color.rgb(red, green, 0)
        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE

        canvas.drawLine(ball.x, ball.y, dragCurrentX, dragCurrentY, paint)

        // Стрелка направления
        val angle = atan2(dy, dx)
        val arrowLength = 30f
        val arrowAngle = 30f * kotlin.math.PI / 180f

        val arrowX1 = dragCurrentX - cos(angle - arrowAngle).toFloat() * arrowLength
        val arrowY1 = dragCurrentY - sin(angle - arrowAngle).toFloat() * arrowLength
        val arrowX2 = dragCurrentX - cos(angle + arrowAngle).toFloat() * arrowLength
        val arrowY2 = dragCurrentY - sin(angle + arrowAngle).toFloat() * arrowLength

        canvas.drawLine(dragCurrentX, dragCurrentY, arrowX1, arrowY1, paint)
        canvas.drawLine(dragCurrentX, dragCurrentY, arrowX2, arrowY2, paint)
    }

    private fun drawBall(canvas: Canvas) {
        canvas.save()
        canvas.translate(ball.x, ball.y)
        canvas.rotate(ball.rotation)

        // Основной цвет мяча
        paint.color = Color.parseColor("#FF8C00")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 0f, ball.radius, paint)

        // Линии на мяче
        paint.color = Color.parseColor("#000000")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        // Вертикальная линия
        canvas.drawLine(0f, -ball.radius, 0f, ball.radius, paint)
        // Горизонтальная линия
        canvas.drawLine(-ball.radius, 0f, ball.radius, 0f, paint)
        // Диагональные линии
        val diagonalOffset = ball.radius * 0.7f
        canvas.drawLine(-diagonalOffset, -diagonalOffset, diagonalOffset, diagonalOffset, paint)
        canvas.drawLine(-diagonalOffset, diagonalOffset, diagonalOffset, -diagonalOffset, paint)

        canvas.restore()
    }

    private fun finishGame() {
        running = false
        gameHasStarted = false
        removeCallbacks(frameRunnable)
        invalidate()
        listener?.onGameOver(score, shotsMade, totalShots)
    }

    private fun setupBall() {
        val radius = BALL_RADIUS_DP * density
        ball = Ball(
            x = viewWidth * 0.2f,
            y = viewHeight * 0.85f,
            radius = radius,
            velocityX = 0f,
            velocityY = 0f,
            rotation = 0f,
            rotationSpeed = 0f
        )
    }

    private fun setupHoop() {
        hoop = Hoop(
            centerX = viewWidth * 0.8f,
            centerY = viewHeight * 0.3f,
            radius = HOOP_RADIUS_DP * density
        )
    }

    private data class Ball(
        var x: Float,
        var y: Float,
        var radius: Float,
        var velocityX: Float,
        var velocityY: Float,
        var rotation: Float,
        var rotationSpeed: Float
    )

    private data class Hoop(
        var centerX: Float,
        var centerY: Float,
        var radius: Float
    )

    companion object {
        private const val FRAME_DELAY_MS = 16L
        private const val MAX_FRAME_DELTA = 80L

        private const val GAME_TIME_SECONDS = 60
        private const val BASKET_SCORE = 10

        private const val GRAVITY = 800f
        private const val MAX_SHOOT_POWER = 600f

        private const val BALL_RADIUS_DP = 25f
        private const val HOOP_RADIUS_DP = 35f
    }
}

