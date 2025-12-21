package mikle.sam.spacerunner.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SpaceRunnerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    interface Listener {
        fun onScoreChanged(score: Int)
        fun onDistanceChanged(distance: Int)
        fun onSpeedChanged(speed: Int)
        fun onGameOver(finalScore: Int, finalDistance: Int)
    }

    private val obstacles = mutableListOf<Obstacle>()
    private val stars = mutableListOf<Star>()
    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shipPath = Path()
    private val random = Random(System.currentTimeMillis())

    private val density = resources.displayMetrics.density
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var listener: Listener? = null

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var lastFrameTime = 0L
    private var lastSpawnTime = 0L
    private var lastStarSpawnTime = 0L
    private var gameStartTime = 0L
    private var running = false
    private var gameHasStarted = false

    private var score = 0
    private var distance = 0
    private var speed = 100 // км/ч

    private var ship = Ship()
    private var shipVelocityX = 0f
    private var accelerometerX = 0f
    private var backgroundOffset = 0f

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
        obstacles.clear()
        stars.clear()
        particles.clear()
        score = 0
        distance = 0
        speed = 100
        listener?.onScoreChanged(score)
        listener?.onDistanceChanged(distance)
        listener?.onSpeedChanged(speed)

        ship.centerX = viewWidth / 2f
        shipVelocityX = 0f
        accelerometerX = 0f
        backgroundOffset = 0f

        gameHasStarted = true
        running = true
        val now = SystemClock.elapsedRealtime()
        gameStartTime = now
        lastFrameTime = now
        lastSpawnTime = now
        lastStarSpawnTime = now

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun pauseGame() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
        removeCallbacks(frameRunnable)
    }

    fun resumeGame() {
        if (!gameHasStarted || running) return
        running = true
        lastFrameTime = SystemClock.elapsedRealtime()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun stopGame() {
        running = false
        gameHasStarted = false
        sensorManager.unregisterListener(this)
        removeCallbacks(frameRunnable)
        obstacles.clear()
        stars.clear()
        particles.clear()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        setupShip()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем звездный фон
        drawStarfield(canvas)

        // Рисуем препятствия
        for (obstacle in obstacles) {
            drawObstacle(canvas, obstacle)
        }

        // Рисуем звезды для сбора
        for (star in stars) {
            drawStar(canvas, star)
        }

        // Рисуем частицы
        for (particle in particles) {
            drawParticle(canvas, particle)
        }

        // Рисуем корабль
        drawShip(canvas)

        // Рисуем след от двигателя
        drawEngineTrail(canvas)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && running) {
            // Используем X-ось для управления (наклон влево/вправо)
            accelerometerX = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager.unregisterListener(this)
        removeCallbacks(frameRunnable)
    }

    private fun updateGame() {
        val now = SystemClock.elapsedRealtime()
        val deltaMillis = (now - lastFrameTime).coerceAtMost(MAX_FRAME_DELTA)
        lastFrameTime = now
        val deltaSeconds = deltaMillis / 1000f

        // Обновляем скорость и дистанцию
        updateProgress(now, deltaSeconds)

        // Обновляем корабль
        updateShip(deltaSeconds)

        // Обновляем препятствия
        updateObstacles(deltaSeconds)

        // Обновляем звезды
        updateStars(deltaSeconds)

        // Обновляем частицы
        updateParticles(deltaSeconds)

        // Проверяем столкновения
        checkCollisions()

        // Спавним новые объекты
        val obstacleSpawnInterval = getObstacleSpawnInterval()
        if (now - lastSpawnTime >= obstacleSpawnInterval) {
            spawnObstacle()
            lastSpawnTime = now
        }

        val starSpawnInterval = getStarSpawnInterval()
        if (now - lastStarSpawnTime >= starSpawnInterval) {
            spawnStar()
            lastStarSpawnTime = now
        }
    }

    private fun updateProgress(now: Long, deltaSeconds: Float) {
        // Увеличиваем скорость со временем
        val gameDuration = (now - gameStartTime) / 1000f
        speed = (100 + gameDuration * SPEED_INCREASE_PER_SECOND).toInt()
        listener?.onSpeedChanged(speed)

        // Обновляем дистанцию
        val distanceIncrease = (speed * deltaSeconds * DISTANCE_MULTIPLIER).toInt()
        distance += distanceIncrease
        listener?.onDistanceChanged(distance)
    }

    private fun updateShip(deltaSeconds: Float) {
        // Применяем ускорение от акселерометра
        val targetVelocity = -accelerometerX * SHIP_SENSITIVITY
        val acceleration = (targetVelocity - shipVelocityX) * SHIP_ACCELERATION
        shipVelocityX += acceleration * deltaSeconds

        // Ограничиваем максимальную скорость
        shipVelocityX = shipVelocityX.coerceIn(-MAX_SHIP_VELOCITY, MAX_SHIP_VELOCITY)

        // Обновляем позицию корабля
        ship.centerX += shipVelocityX * deltaSeconds
        ship.centerX = ship.centerX.coerceIn(ship.width / 2f, viewWidth - ship.width / 2f)

        // Обновляем наклон корабля для визуального эффекта
        ship.rotation = shipVelocityX * SHIP_ROTATION_FACTOR
    }

    private fun updateObstacles(deltaSeconds: Float) {
        val speedMultiplier = speed / 100f
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.y += obstacle.speed * deltaSeconds * speedMultiplier
            obstacle.rotation += obstacle.rotationSpeed * deltaSeconds

            if (obstacle.y - obstacle.size > viewHeight) {
                iterator.remove()
                // Начисляем очки за пройденное препятствие
                score += OBSTACLE_PASS_SCORE
                listener?.onScoreChanged(score)
            }
        }
    }

    private fun updateStars(deltaSeconds: Float) {
        val speedMultiplier = speed / 100f
        val iterator = stars.iterator()
        while (iterator.hasNext()) {
            val star = iterator.next()
            star.y += star.speed * deltaSeconds * speedMultiplier
            star.rotation += star.rotationSpeed * deltaSeconds

            if (star.y - star.size > viewHeight) {
                iterator.remove()
            }
        }
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

    private fun checkCollisions() {
        // Проверка столкновений с препятствиями
        val obstacleIterator = obstacles.iterator()
        while (obstacleIterator.hasNext()) {
            val obstacle = obstacleIterator.next()
            if (checkCollision(ship, obstacle.x, obstacle.y, obstacle.size)) {
                obstacleIterator.remove()
                createExplosion(ship.centerX, ship.y)
                finishGame()
                return
            }
        }

        // Проверка сбора звезд
        val starIterator = stars.iterator()
        while (starIterator.hasNext()) {
            val star = starIterator.next()
            if (checkCollision(ship, star.x, star.y, star.size)) {
                starIterator.remove()
                score += STAR_SCORE
                listener?.onScoreChanged(score)
                createStarCollectionEffect(star.x, star.y)
            }
        }
    }

    private fun checkCollision(ship: Ship, x: Float, y: Float, size: Float): Boolean {
        val dx = x - ship.centerX
        val dy = y - ship.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return distance <= (ship.width / 2f + size / 2f)
    }

    private fun spawnObstacle() {
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val obstacleType = ObstacleType.values()[random.nextInt(ObstacleType.values().size)]
        val size = when (obstacleType) {
            ObstacleType.SMALL -> min(viewWidth, viewHeight) * 0.12f
            ObstacleType.MEDIUM -> min(viewWidth, viewHeight) * 0.18f
            ObstacleType.LARGE -> min(viewWidth, viewHeight) * 0.25f
        }

        val x = random.nextFloat() * (viewWidth - size) + size / 2f
        val baseSpeed = min(viewWidth, viewHeight) * 0.5f
        val rotationSpeed = (random.nextFloat() - 0.5f) * 180f

        obstacles.add(
            Obstacle(
                x = x,
                y = -size,
                size = size,
                speed = baseSpeed,
                rotation = 0f,
                rotationSpeed = rotationSpeed,
                type = obstacleType
            )
        )
    }

    private fun spawnStar() {
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val size = min(viewWidth, viewHeight) * 0.08f
        val x = random.nextFloat() * (viewWidth - size) + size / 2f
        val baseSpeed = min(viewWidth, viewHeight) * 0.4f
        val rotationSpeed = random.nextFloat() * 360f

        stars.add(
            Star(
                x = x,
                y = -size,
                size = size,
                speed = baseSpeed,
                rotation = 0f,
                rotationSpeed = rotationSpeed
            )
        )
    }

    private fun getObstacleSpawnInterval(): Long {
        val baseInterval = OBSTACLE_SPAWN_INTERVAL_MS
        val reduction = (speed - 100) / 10 * OBSTACLE_SPAWN_REDUCTION_PER_SPEED
        return (baseInterval - reduction).coerceAtLeast(MIN_OBSTACLE_SPAWN_INTERVAL_MS)
    }

    private fun getStarSpawnInterval(): Long {
        return STAR_SPAWN_INTERVAL_MS
    }

    private fun createExplosion(x: Float, y: Float) {
        for (i in 0 until EXPLOSION_PARTICLES) {
            val angle = (i.toFloat() / EXPLOSION_PARTICLES) * 360f * kotlin.math.PI / 180f
            val velocity = random.nextFloat() * EXPLOSION_VELOCITY + EXPLOSION_VELOCITY_MIN
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    velocityX = (kotlin.math.cos(angle) * velocity).toFloat(),
                    velocityY = (kotlin.math.sin(angle) * velocity).toFloat(),
                    color = Color.parseColor("#FF4444"),
                    size = random.nextFloat() * 8f + 4f,
                    life = random.nextFloat() * 0.5f + 0.3f,
                    maxLife = 0.8f,
                    alpha = 255
                )
            )
        }
    }

    private fun createStarCollectionEffect(x: Float, y: Float) {
        for (i in 0 until STAR_COLLECTION_PARTICLES) {
            val angle = (i.toFloat() / STAR_COLLECTION_PARTICLES) * 360f * kotlin.math.PI / 180f
            val velocity = random.nextFloat() * STAR_COLLECTION_VELOCITY + STAR_COLLECTION_VELOCITY_MIN
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    velocityX = (kotlin.math.cos(angle) * velocity).toFloat(),
                    velocityY = (kotlin.math.sin(angle) * velocity).toFloat(),
                    color = Color.parseColor("#FFD700"),
                    size = random.nextFloat() * 6f + 3f,
                    life = random.nextFloat() * 0.4f + 0.2f,
                    maxLife = 0.6f,
                    alpha = 255
                )
            )
        }
    }

    private fun drawStarfield(canvas: Canvas) {
        // Простой звездный фон
        paint.color = Color.WHITE
        paint.alpha = 100
        paint.style = Paint.Style.FILL

        backgroundOffset += speed * 0.01f
        if (backgroundOffset > viewHeight) {
            backgroundOffset = 0f
        }

        // Рисуем звезды фона
        for (i in 0 until 50) {
            val starX = (i * 37.5f) % viewWidth
            val starY = (i * 23.7f + backgroundOffset) % viewHeight
            canvas.drawCircle(starX, starY, 2f, paint)
        }

        paint.alpha = 255
    }

    private fun drawShip(canvas: Canvas) {
        canvas.save()
        canvas.translate(ship.centerX, ship.y)
        canvas.rotate(ship.rotation)

        paint.style = Paint.Style.FILL

        // Корпус корабля
        shipPath.reset()
        shipPath.moveTo(0f, -ship.height / 2f)
        shipPath.lineTo(-ship.width / 2f, ship.height / 2f)
        shipPath.lineTo(0f, ship.height / 2f - ship.height * 0.2f)
        shipPath.lineTo(ship.width / 2f, ship.height / 2f)
        shipPath.close()

        paint.color = Color.parseColor("#4A9EFF")
        canvas.drawPath(shipPath, paint)

        // Окно кабины
        paint.color = Color.parseColor("#2D5F8F")
        canvas.drawCircle(0f, -ship.height * 0.15f, ship.width * 0.15f, paint)

        // Детали
        paint.color = Color.parseColor("#6AB0FF")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawPath(shipPath, paint)

        canvas.restore()
    }

    private fun drawEngineTrail(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.alpha = 150

        val trailY = ship.y + ship.height / 2f
        val trailWidth = ship.width * 0.6f
        val trailHeight = ship.height * 0.4f

        // Основной след
        paint.color = Color.parseColor("#FF6A00")
        canvas.drawRect(
            ship.centerX - trailWidth / 2f,
            trailY,
            ship.centerX + trailWidth / 2f,
            trailY + trailHeight,
            paint
        )

        // Яркое ядро
        paint.color = Color.parseColor("#FFD700")
        paint.alpha = 200
        canvas.drawRect(
            ship.centerX - trailWidth / 4f,
            trailY,
            ship.centerX + trailWidth / 4f,
            trailY + trailHeight * 0.6f,
            paint
        )

        paint.alpha = 255
    }

    private fun drawObstacle(canvas: Canvas, obstacle: Obstacle) {
        canvas.save()
        canvas.translate(obstacle.x, obstacle.y)
        canvas.rotate(obstacle.rotation)

        paint.style = Paint.Style.FILL

        when (obstacle.type) {
            ObstacleType.SMALL -> {
                // Маленький астероид
                paint.color = Color.parseColor("#FF6666")
                canvas.drawCircle(0f, 0f, obstacle.size / 2f, paint)
                paint.color = Color.parseColor("#CC0000")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(0f, 0f, obstacle.size / 2f, paint)
            }
            ObstacleType.MEDIUM -> {
                // Средний астероид (многоугольник)
                paint.color = Color.parseColor("#FF4444")
                val path = Path()
                val sides = 6
                for (i in 0 until sides) {
                    val angle = (i * 360f / sides) * kotlin.math.PI / 180f
                    val x = (kotlin.math.cos(angle) * obstacle.size / 2f).toFloat()
                    val y = (kotlin.math.sin(angle) * obstacle.size / 2f).toFloat()
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ObstacleType.LARGE -> {
                // Большой астероид (неправильная форма)
                paint.color = Color.parseColor("#FF2222")
                val path = Path()
                val sides = 8
                for (i in 0 until sides) {
                    val angle = (i * 360f / sides) * kotlin.math.PI / 180f
                    val radius = obstacle.size / 2f * (0.7f + random.nextFloat() * 0.3f)
                    val x = (kotlin.math.cos(angle) * radius).toFloat()
                    val y = (kotlin.math.sin(angle) * radius).toFloat()
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        canvas.restore()
    }

    private fun drawStar(canvas: Canvas, star: Star) {
        canvas.save()
        canvas.translate(star.x, star.y)
        canvas.rotate(star.rotation)

        paint.style = Paint.Style.FILL

        // Внешняя звезда
        paint.color = Color.parseColor("#FFD700")
        val path = Path()
        val points = 5
        for (i in 0 until points * 2) {
            val angle = (i * 360f / (points * 2)) * kotlin.math.PI / 180f
            val radius = if (i % 2 == 0) star.size / 2f else star.size / 4f
            val x = (kotlin.math.cos(angle) * radius).toFloat()
            val y = (kotlin.math.sin(angle) * radius).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, paint)

        // Внутренняя звезда
        paint.color = Color.parseColor("#FFA500")
        val innerPath = Path()
        for (i in 0 until points * 2) {
            val angle = (i * 360f / (points * 2)) * kotlin.math.PI / 180f
            val radius = if (i % 2 == 0) star.size / 4f else star.size / 6f
            val x = (kotlin.math.cos(angle) * radius).toFloat()
            val y = (kotlin.math.sin(angle) * radius).toFloat()
            if (i == 0) {
                innerPath.moveTo(x, y)
            } else {
                innerPath.lineTo(x, y)
            }
        }
        innerPath.close()
        canvas.drawPath(innerPath, paint)

        canvas.restore()
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
        sensorManager.unregisterListener(this)
        removeCallbacks(frameRunnable)
        obstacles.clear()
        stars.clear()
        invalidate()
        listener?.onGameOver(score, distance)
    }

    private fun setupShip() {
        val width = max(viewWidth * SHIP_WIDTH_RATIO, MIN_SHIP_WIDTH_DP * density)
        val height = width * SHIP_HEIGHT_RATIO
        val y = viewHeight - height - SHIP_BOTTOM_PADDING_DP * density
        ship = Ship(
            centerX = viewWidth / 2f,
            y = y,
            width = width,
            height = height,
            rotation = 0f
        )
    }

    private enum class ObstacleType {
        SMALL,
        MEDIUM,
        LARGE
    }

    private data class Ship(
        var centerX: Float = 0f,
        var y: Float = 0f,
        var width: Float = 0f,
        var height: Float = 0f,
        var rotation: Float = 0f
    )

    private data class Obstacle(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var type: ObstacleType
    )

    private data class Star(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var rotation: Float,
        var rotationSpeed: Float
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
        private const val OBSTACLE_SPAWN_INTERVAL_MS = 1500L
        private const val MIN_OBSTACLE_SPAWN_INTERVAL_MS = 600L
        private const val OBSTACLE_SPAWN_REDUCTION_PER_SPEED = 10L
        private const val STAR_SPAWN_INTERVAL_MS = 2000L
        private const val MAX_FRAME_DELTA = 80L

        private const val OBSTACLE_PASS_SCORE = 5
        private const val STAR_SCORE = 20

        private const val SPEED_INCREASE_PER_SECOND = 2f
        private const val DISTANCE_MULTIPLIER = 0.1f

        private const val SHIP_SENSITIVITY = 50f
        private const val SHIP_ACCELERATION = 8f
        private const val MAX_SHIP_VELOCITY = 400f
        private const val SHIP_ROTATION_FACTOR = 0.1f

        private const val EXPLOSION_PARTICLES = 15
        private const val EXPLOSION_VELOCITY = 300f
        private const val EXPLOSION_VELOCITY_MIN = 100f

        private const val STAR_COLLECTION_PARTICLES = 10
        private const val STAR_COLLECTION_VELOCITY = 200f
        private const val STAR_COLLECTION_VELOCITY_MIN = 50f

        private const val SHIP_WIDTH_RATIO = 0.15f
        private const val SHIP_HEIGHT_RATIO = 0.4f
        private const val MIN_SHIP_WIDTH_DP = 60f
        private const val SHIP_BOTTOM_PADDING_DP = 32f
    }
}

