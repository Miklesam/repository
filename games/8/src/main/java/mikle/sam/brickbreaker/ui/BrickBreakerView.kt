package mikle.sam.brickbreaker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class BrickBreakerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onScoreChanged(score: Int)
        fun onLivesChanged(lives: Int)
        fun onLevelChanged(level: Int)
        fun onGameOver(finalScore: Int, finalLevel: Int)
        fun onLevelComplete(level: Int)
    }

    private val bricks = mutableListOf<Brick>()
    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paddleRect = RectF()
    private val ballRect = RectF()
    private val random = Random(System.currentTimeMillis())

    private val density = resources.displayMetrics.density

    private var listener: Listener? = null

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var lastFrameTime = 0L
    private var running = false
    private var gameHasStarted = false
    private var ballLaunched = false

    private var score = 0
    private var lives = START_LIVES
    private var level = 1

    private var paddle = Paddle()
    private var ball = Ball()
    private var targetPaddleX = 0f
    private var touchX = 0f
    private var isTouching = false

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
        bricks.clear()
        particles.clear()
        score = 0
        lives = START_LIVES
        level = 1
        ballLaunched = false
        listener?.onScoreChanged(score)
        listener?.onLivesChanged(lives)
        listener?.onLevelChanged(level)

        setupPaddle()
        setupBall()
        createLevel(level)

        gameHasStarted = true
        running = true
        lastFrameTime = SystemClock.elapsedRealtime()

        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun pauseGame() {
        if (!running) return
        running = false
        removeCallbacks(frameRunnable)
    }

    fun resumeGame() {
        if (!gameHasStarted || running || lives <= 0) return
        running = true
        lastFrameTime = SystemClock.elapsedRealtime()
        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun stopGame() {
        running = false
        gameHasStarted = false
        removeCallbacks(frameRunnable)
        bricks.clear()
        particles.clear()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        setupPaddle()
        setupBall()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем блоки
        for (brick in bricks) {
            drawBrick(canvas, brick)
        }

        // Рисуем частицы
        for (particle in particles) {
            drawParticle(canvas, particle)
        }

        // Рисуем платформу
        drawPaddle(canvas)

        // Рисуем мяч
        drawBall(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                isTouching = true
                if (!ballLaunched && running) {
                    launchBall()
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isTouching = false
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

        updatePaddle(deltaSeconds)
        if (ballLaunched) {
            updateBall(deltaSeconds)
            checkCollisions()
        } else {
            // Мяч следует за платформой до запуска
            ball.x = paddle.centerX
            ball.y = paddle.y - ball.radius - 2f
        }

        updateParticles(deltaSeconds)

        // Проверяем завершение уровня
        if (bricks.isEmpty() && ballLaunched) {
            completeLevel()
        }
    }

    private fun updatePaddle(deltaSeconds: Float) {
        if (isTouching) {
            targetPaddleX = touchX.coerceIn(paddle.width / 2f, viewWidth - paddle.width / 2f)
        }

        val distance = targetPaddleX - paddle.centerX
        if (abs(distance) < PADDLE_SPEED * deltaSeconds) {
            paddle.centerX = targetPaddleX
        } else {
            val direction = if (distance > 0) 1 else -1
            paddle.centerX += direction * PADDLE_SPEED * deltaSeconds
        }
    }

    private fun updateBall(deltaSeconds: Float) {
        ball.x += ball.velocityX * deltaSeconds
        ball.y += ball.velocityY * deltaSeconds

        // Отскок от стен
        if (ball.x - ball.radius <= 0f || ball.x + ball.radius >= viewWidth) {
            ball.velocityX = -ball.velocityX
            ball.x = ball.x.coerceIn(ball.radius, viewWidth - ball.radius)
        }

        if (ball.y - ball.radius <= 0f) {
            ball.velocityY = -ball.velocityY
            ball.y = ball.radius
        }

        // Мяч упал вниз
        if (ball.y + ball.radius >= viewHeight) {
            loseLife()
        }
    }

    private fun checkCollisions() {
        // Столкновение с платформой
        if (ball.y + ball.radius >= paddle.y &&
            ball.y - ball.radius <= paddle.y + paddle.height &&
            ball.x + ball.radius >= paddle.centerX - paddle.width / 2f &&
            ball.x - ball.radius <= paddle.centerX + paddle.width / 2f &&
            ball.velocityY > 0
        ) {
            // Вычисляем точку удара относительно центра платформы
            val hitPos = (ball.x - paddle.centerX) / (paddle.width / 2f)
            val angle = hitPos * MAX_BOUNCE_ANGLE

            val speed = kotlin.math.sqrt(
                (ball.velocityX * ball.velocityX + ball.velocityY * ball.velocityY).toDouble()
            ).toFloat()

            ball.velocityX = kotlin.math.sin(angle * kotlin.math.PI / 180f).toFloat() * speed
            ball.velocityY = -kotlin.math.cos(angle * kotlin.math.PI / 180f).toFloat() * speed

            ball.y = paddle.y - ball.radius
        }

        // Столкновение с блоками
        val brickIterator = bricks.iterator()
        while (brickIterator.hasNext()) {
            val brick = brickIterator.next()
            if (checkBallBrickCollision(ball, brick)) {
                brickIterator.remove()
                score += BRICK_SCORE * brick.hits
                listener?.onScoreChanged(score)
                createBrickBreakEffect(brick)
            }
        }
    }

    private fun checkBallBrickCollision(ball: Ball, brick: Brick): Boolean {
        // Упрощенная проверка столкновения AABB с кругом
        val closestX = ball.x.coerceIn(brick.x, brick.x + brick.width)
        val closestY = ball.y.coerceIn(brick.y, brick.y + brick.height)

        val dx = ball.x - closestX
        val dy = ball.y - closestY
        val distanceSquared = dx * dx + dy * dy

        if (distanceSquared < ball.radius * ball.radius) {
            // Определяем сторону столкновения
            val overlapX = ball.radius - abs(dx)
            val overlapY = ball.radius - abs(dy)

            if (overlapX < overlapY) {
                ball.velocityX = -ball.velocityX
                ball.x += if (dx > 0) overlapX else -overlapX
            } else {
                ball.velocityY = -ball.velocityY
                ball.y += if (dy > 0) overlapY else -overlapY
            }

            brick.hits--
            return brick.hits <= 0
        }
        return false
    }

    private fun updateParticles(deltaSeconds: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.velocityX * deltaSeconds
            particle.y += particle.velocityY * deltaSeconds
            particle.life -= deltaSeconds
            particle.alpha = (particle.life / particle.maxLife * 255).toInt().coerceIn(0, 255)

            if (particle.life <= 0) {
                iterator.remove()
            }
        }
    }

    private fun launchBall() {
        if (ballLaunched) return
        ballLaunched = true
        val angle = (random.nextFloat() * 60f + 60f) * kotlin.math.PI / 180f // 60-120 градусов
        val speed = BALL_SPEED
        ball.velocityX = kotlin.math.sin(angle).toFloat() * speed * (if (random.nextBoolean()) 1f else -1f)
        ball.velocityY = -kotlin.math.cos(angle).toFloat() * speed
    }

    private fun loseLife() {
        if (!running) return

        lives--
        listener?.onLivesChanged(lives)

        if (lives <= 0) {
            finishGame()
        } else {
            // Перезапускаем мяч
            ballLaunched = false
            setupBall()
        }
    }

    private fun completeLevel() {
        running = false
        ballLaunched = false
        val completedLevel = level
        level++
        listener?.onLevelChanged(level)
        createLevel(level)
        setupBall()
        listener?.onLevelComplete(completedLevel)
        
        // Продолжаем игру через 2 секунды
        postDelayed({
            if (gameHasStarted && lives > 0) {
                running = true
                lastFrameTime = SystemClock.elapsedRealtime()
                removeCallbacks(frameRunnable)
                post(frameRunnable)
            }
        }, 2000)
    }

    private fun createLevel(levelNum: Int) {
        bricks.clear()

        val rows = min(4 + levelNum / 2, 8)
        val cols = 8
        val brickWidth = (viewWidth - (cols + 1) * BRICK_MARGIN) / cols
        val brickHeight = brickWidth * 0.4f
        val startY = viewHeight * 0.15f

        val brickColors = listOf(
            Color.parseColor("#E94560"),
            Color.parseColor("#FF6B9D"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#4ECDC4")
        )

        for (row in 0 until rows) {
            val hits = (row / 2) + 1
            val color = brickColors[row % brickColors.size]
            for (col in 0 until cols) {
                val x = BRICK_MARGIN + col * (brickWidth + BRICK_MARGIN)
                val y = startY + row * (brickHeight + BRICK_MARGIN)

                bricks.add(
                    Brick(
                        x = x,
                        y = y,
                        width = brickWidth,
                        height = brickHeight,
                        color = color,
                        hits = hits
                    )
                )
            }
        }
    }

    private fun createBrickBreakEffect(brick: Brick) {
        val centerX = brick.x + brick.width / 2f
        val centerY = brick.y + brick.height / 2f

        for (i in 0 until BRICK_BREAK_PARTICLES) {
            val angle = (i.toFloat() / BRICK_BREAK_PARTICLES) * 360f * kotlin.math.PI / 180f
            val velocity = random.nextFloat() * PARTICLE_VELOCITY + PARTICLE_VELOCITY_MIN
            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    velocityX = (kotlin.math.cos(angle) * velocity).toFloat(),
                    velocityY = (kotlin.math.sin(angle) * velocity).toFloat(),
                    color = brick.color,
                    size = random.nextFloat() * 6f + 3f,
                    life = random.nextFloat() * 0.5f + 0.3f,
                    maxLife = 0.8f,
                    alpha = 255
                )
            )
        }
    }

    private fun drawPaddle(canvas: Canvas) {
        paddleRect.set(
            paddle.centerX - paddle.width / 2f,
            paddle.y,
            paddle.centerX + paddle.width / 2f,
            paddle.y + paddle.height
        )

        paint.color = Color.parseColor("#4A9EFF")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(paddleRect, paddle.height / 2f, paddle.height / 2f, paint)

        // Обводка
        paint.color = Color.parseColor("#2D5F8F")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(paddleRect, paddle.height / 2f, paddle.height / 2f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawBall(canvas: Canvas) {
        paint.color = Color.parseColor("#FFD700")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint)

        // Блик
        paint.color = Color.parseColor("#FFFFFF")
        paint.alpha = 150
        canvas.drawCircle(
            ball.x - ball.radius * 0.3f,
            ball.y - ball.radius * 0.3f,
            ball.radius * 0.3f,
            paint
        )
        paint.alpha = 255
    }

    private fun drawBrick(canvas: Canvas, brick: Brick) {
        val rect = RectF(brick.x, brick.y, brick.x + brick.width, brick.y + brick.height)

        paint.color = brick.color
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        // Обводка
        paint.color = Color.argb(150, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Показываем количество ударов для блоков с несколькими жизнями
        if (brick.hits > 1) {
            paint.color = Color.WHITE
            paint.textSize = brick.height * 0.4f
            paint.textAlign = Paint.Align.CENTER
            val textY = brick.y + brick.height / 2f + paint.textSize / 3f
            canvas.drawText(brick.hits.toString(), brick.x + brick.width / 2f, textY, paint)
        }
    }

    private fun drawParticle(canvas: Canvas, particle: Particle) {
        paint.style = Paint.Style.FILL
        paint.color = particle.color
        paint.alpha = particle.alpha
        canvas.drawCircle(particle.x, particle.y, particle.size, paint)
        paint.alpha = 255
    }

    private fun finishGame() {
        running = false
        gameHasStarted = false
        ballLaunched = false
        removeCallbacks(frameRunnable)
        bricks.clear()
        particles.clear()
        invalidate()
        listener?.onGameOver(score, level)
    }

    private fun setupPaddle() {
        val width = max(viewWidth * PADDLE_WIDTH_RATIO, MIN_PADDLE_WIDTH_DP * density)
        val height = width * PADDLE_HEIGHT_RATIO
        val y = viewHeight - height - PADDLE_BOTTOM_PADDING_DP * density
        paddle = Paddle(
            centerX = viewWidth / 2f,
            y = y,
            width = width,
            height = height
        )
        targetPaddleX = paddle.centerX
    }

    private fun setupBall() {
        val radius = BALL_RADIUS_DP * density
        ball = Ball(
            x = paddle.centerX,
            y = paddle.y - radius - 2f,
            radius = radius,
            velocityX = 0f,
            velocityY = 0f
        )
    }

    private data class Paddle(
        var centerX: Float = 0f,
        var y: Float = 0f,
        var width: Float = 0f,
        var height: Float = 0f
    )

    private data class Ball(
        var x: Float = 0f,
        var y: Float = 0f,
        var radius: Float = 0f,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f
    )

    private data class Brick(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var color: Int,
        var hits: Int = 1
    )

    private data class Particle(
        var x: Float,
        var y: Float,
        var velocityX: Float,
        var velocityY: Float,
        var color: Int,
        var size: Float,
        var life: Float,
        val maxLife: Float,
        var alpha: Int
    )

    companion object {
        private const val FRAME_DELAY_MS = 16L
        private const val MAX_FRAME_DELTA = 80L

        private const val START_LIVES = 3
        private const val BRICK_SCORE = 10

        private const val PADDLE_SPEED = 500f
        private const val BALL_SPEED = 400f
        private const val MAX_BOUNCE_ANGLE = 60f

        private const val BRICK_MARGIN = 8f
        private const val BRICK_BREAK_PARTICLES = 8
        private const val PARTICLE_VELOCITY = 200f
        private const val PARTICLE_VELOCITY_MIN = 50f

        private const val PADDLE_WIDTH_RATIO = 0.25f
        private const val PADDLE_HEIGHT_RATIO = 0.08f
        private const val MIN_PADDLE_WIDTH_DP = 120f
        private const val PADDLE_BOTTOM_PADDING_DP = 40f

        private const val BALL_RADIUS_DP = 12f
    }
}

