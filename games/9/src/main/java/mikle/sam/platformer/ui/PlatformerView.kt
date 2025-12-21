package mikle.sam.platformer.ui

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

class PlatformerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onScoreChanged(score: Int)
        fun onDistanceChanged(distance: Int)
        fun onGameOver(finalScore: Int, finalDistance: Int)
    }

    private val platforms = mutableListOf<Platform>()
    private val coins = mutableListOf<Coin>()
    private val spikes = mutableListOf<Spike>()
    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerRect = RectF()
    private val random = Random(System.currentTimeMillis())

    private val density = resources.displayMetrics.density

    private var listener: Listener? = null

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var lastFrameTime = 0L
    private var running = false
    private var gameHasStarted = false

    private var score = 0
    private var distance = 0
    private var cameraX = 0f
    private var lastPlatformX = 0f

    private var player = Player()
    private var playerVelocityY = 0f
    private var isJumping = false
    private var isOnGround = false

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
        platforms.clear()
        coins.clear()
        spikes.clear()
        particles.clear()
        score = 0
        distance = 0
        cameraX = 0f
        lastPlatformX = 0f
        listener?.onScoreChanged(score)
        listener?.onDistanceChanged(distance)

        setupPlayer()
        createInitialPlatforms()

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
        platforms.clear()
        coins.clear()
        spikes.clear()
        particles.clear()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        setupPlayer()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Смещаем канвас для камеры
        canvas.save()
        canvas.translate(-cameraX, 0f)

        // Рисуем платформы
        for (platform in platforms) {
            drawPlatform(canvas, platform)
        }

        // Рисуем шипы
        for (spike in spikes) {
            drawSpike(canvas, spike)
        }

        // Рисуем монеты
        for (coin in coins) {
            drawCoin(canvas, coin)
        }

        // Рисуем частицы
        for (particle in particles) {
            drawParticle(canvas, particle)
        }

        // Рисуем игрока
        drawPlayer(canvas)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isOnGround && !isJumping) {
                    jump()
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

        updatePlayer(deltaSeconds)
        updateCamera()
        updatePlatforms(deltaSeconds)
        updateCoins(deltaSeconds)
        updateSpikes(deltaSeconds)
        updateParticles(deltaSeconds)
        checkCollisions()
        generateNewPlatforms()
        cleanupOffscreenObjects()

        // Обновляем дистанцию
        val distanceIncrease = (player.x - cameraX) * DISTANCE_MULTIPLIER
        if (distanceIncrease > 0) {
            distance += distanceIncrease.toInt()
            listener?.onDistanceChanged(distance)
        }
    }

    private fun updatePlayer(deltaSeconds: Float) {
        // Автоматическое движение вперед
        player.x += PLAYER_FORWARD_SPEED * deltaSeconds

        // Применяем гравитацию
        if (!isOnGround) {
            playerVelocityY += GRAVITY * deltaSeconds
            playerVelocityY = min(playerVelocityY, MAX_FALL_SPEED)
        }

        // Обновляем позицию
        player.y += playerVelocityY * deltaSeconds

        // Проверяем столкновение с платформами
        isOnGround = false
        for (platform in platforms) {
            if (checkPlayerPlatformCollision(player, platform)) {
                isOnGround = true
                isJumping = false
                playerVelocityY = 0f
                player.y = platform.y - player.height
                break
            }
        }

        // Проверяем выход за границы экрана
        if (player.y > viewHeight) {
            finishGame()
        }
    }

    private fun updateCamera() {
        // Камера следует за игроком, но с задержкой
        val targetCameraX = player.x - viewWidth * 0.3f
        if (targetCameraX > cameraX) {
            cameraX = targetCameraX
        }
    }

    private fun updatePlatforms(deltaSeconds: Float) {
        // Платформы не двигаются сами, двигается камера
    }

    private fun updateCoins(deltaSeconds: Float) {
        val iterator = coins.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.rotation += coin.rotationSpeed * deltaSeconds
        }
    }

    private fun updateSpikes(deltaSeconds: Float) {
        // Шипы статичны
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
        // Столкновение с монетами
        val coinIterator = coins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            if (checkPlayerCoinCollision(player, coin)) {
                coinIterator.remove()
                score += COIN_SCORE
                listener?.onScoreChanged(score)
                createCoinCollectionEffect(coin.x, coin.y)
            }
        }

        // Столкновение с шипами
        for (spike in spikes) {
            if (checkPlayerSpikeCollision(player, spike)) {
                finishGame()
                return
            }
        }
    }

    private fun checkPlayerPlatformCollision(player: Player, platform: Platform): Boolean {
        return player.x + player.width > platform.x &&
                player.x < platform.x + platform.width &&
                player.y + player.height > platform.y &&
                player.y + player.height <= platform.y + PLATFORM_COLLISION_MARGIN &&
                playerVelocityY >= 0f
    }

    private fun checkPlayerCoinCollision(player: Player, coin: Coin): Boolean {
        val dx = coin.x - (player.x + player.width / 2f)
        val dy = coin.y - (player.y + player.height / 2f)
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return distance < (player.width / 2f + coin.radius)
    }

    private fun checkPlayerSpikeCollision(player: Player, spike: Spike): Boolean {
        return player.x + player.width > spike.x &&
                player.x < spike.x + spike.width &&
                player.y + player.height > spike.y &&
                player.y < spike.y + spike.height
    }

    private fun jump() {
        if (!isOnGround) return
        isJumping = true
        isOnGround = false
        playerVelocityY = -JUMP_VELOCITY
    }

    private fun createInitialPlatforms() {
        // Создаем стартовую платформу
        val startPlatform = Platform(
            x = 0f,
            y = viewHeight * 0.7f,
            width = viewWidth * 0.6f,
            height = PLATFORM_HEIGHT
        )
        platforms.add(startPlatform)
        lastPlatformX = startPlatform.x + startPlatform.width

        // Создаем несколько платформ впереди
        for (i in 0 until 5) {
            generatePlatform()
        }
    }

    private fun generateNewPlatforms() {
        // Генерируем новые платформы, когда игрок приближается к концу
        val viewRight = cameraX + viewWidth
        while (lastPlatformX < viewRight + viewWidth) {
            generatePlatform()
        }
    }

    private fun generatePlatform() {
        val minGap = PLATFORM_MIN_GAP
        val maxGap = PLATFORM_MAX_GAP
        val gap = random.nextFloat() * (maxGap - minGap) + minGap

        val minWidth = PLATFORM_MIN_WIDTH
        val maxWidth = PLATFORM_MAX_WIDTH
        val width = random.nextFloat() * (maxWidth - minWidth) + minWidth

        val x = lastPlatformX + gap
        val y = random.nextFloat() * (viewHeight * 0.6f - viewHeight * 0.2f) + viewHeight * 0.2f

        val platform = Platform(
            x = x,
            y = y,
            width = width,
            height = PLATFORM_HEIGHT
        )
        platforms.add(platform)
        lastPlatformX = x + width

        // Иногда добавляем монету на платформу
        if (random.nextFloat() < COIN_SPAWN_CHANCE) {
            coins.add(
                Coin(
                    x = x + width / 2f,
                    y = y - COIN_RADIUS - 10f,
                    radius = COIN_RADIUS,
                    rotation = 0f,
                    rotationSpeed = 180f
                )
            )
        }

        // Иногда добавляем шипы на платформу
        if (random.nextFloat() < SPIKE_SPAWN_CHANCE) {
            spikes.add(
                Spike(
                    x = x + width / 2f - SPIKE_WIDTH / 2f,
                    y = y - SPIKE_HEIGHT,
                    width = SPIKE_WIDTH,
                    height = SPIKE_HEIGHT
                )
            )
        }
    }

    private fun cleanupOffscreenObjects() {
        val leftBound = cameraX - viewWidth

        platforms.removeAll { it.x + it.width < leftBound }
        coins.removeAll { it.x + it.radius < leftBound || it.x - it.radius > cameraX + viewWidth * 2f }
        spikes.removeAll { it.x + it.width < leftBound || it.x > cameraX + viewWidth * 2f }
    }

    private fun createCoinCollectionEffect(x: Float, y: Float) {
        for (i in 0 until COIN_COLLECTION_PARTICLES) {
            val angle = (i.toFloat() / COIN_COLLECTION_PARTICLES) * 360f * kotlin.math.PI / 180f
            val velocity = random.nextFloat() * PARTICLE_VELOCITY + PARTICLE_VELOCITY_MIN
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    velocityX = (kotlin.math.cos(angle) * velocity).toFloat(),
                    velocityY = (kotlin.math.sin(angle) * velocity).toFloat(),
                    color = Color.parseColor("#FFD700"),
                    size = random.nextFloat() * 4f + 2f,
                    life = random.nextFloat() * 0.4f + 0.2f,
                    maxLife = 0.6f,
                    alpha = 255
                )
            )
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        playerRect.set(
            player.x,
            player.y,
            player.x + player.width,
            player.y + player.height
        )

        // Тело игрока
        paint.color = Color.parseColor("#FF6347")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(playerRect, player.width * 0.2f, player.width * 0.2f, paint)

        // Голова
        paint.color = Color.parseColor("#FF8C69")
        canvas.drawCircle(
            player.x + player.width / 2f,
            player.y + player.width / 2f,
            player.width / 3f,
            paint
        )

        // Глаза
        paint.color = Color.WHITE
        canvas.drawCircle(
            player.x + player.width / 2f - player.width / 6f,
            player.y + player.width / 2f - player.width / 12f,
            player.width / 12f,
            paint
        )
        canvas.drawCircle(
            player.x + player.width / 2f + player.width / 6f,
            player.y + player.width / 2f - player.width / 12f,
            player.width / 12f,
            paint
        )

        paint.color = Color.BLACK
        canvas.drawCircle(
            player.x + player.width / 2f - player.width / 6f,
            player.y + player.width / 2f - player.width / 12f,
            player.width / 24f,
            paint
        )
        canvas.drawCircle(
            player.x + player.width / 2f + player.width / 6f,
            player.y + player.width / 2f - player.width / 12f,
            player.width / 24f,
            paint
        )
    }

    private fun drawPlatform(canvas: Canvas, platform: Platform) {
        val rect = RectF(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height)

        paint.color = Color.parseColor("#228B22")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        // Трава сверху
        paint.color = Color.parseColor("#32CD32")
        canvas.drawRect(
            platform.x,
            platform.y,
            platform.x + platform.width,
            platform.y + platform.height * 0.3f,
            paint
        )

        // Обводка
        paint.color = Color.parseColor("#006400")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawCoin(canvas: Canvas, coin: Coin) {
        canvas.save()
        canvas.translate(coin.x, coin.y)
        canvas.rotate(coin.rotation)

        paint.color = Color.parseColor("#FFD700")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 0f, coin.radius, paint)

        paint.color = Color.parseColor("#FFA500")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(0f, 0f, coin.radius, paint)

        // Буква C
        paint.color = Color.parseColor("#FF8C00")
        paint.textSize = coin.radius * 1.2f
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        val textY = coin.radius * 0.3f
        canvas.drawText("C", 0f, textY, paint)

        canvas.restore()
    }

    private fun drawSpike(canvas: Canvas, spike: Spike) {
        paint.color = Color.parseColor("#8B0000")
        paint.style = Paint.Style.FILL

        val path = android.graphics.Path()
        path.moveTo(spike.x + spike.width / 2f, spike.y)
        path.lineTo(spike.x, spike.y + spike.height)
        path.lineTo(spike.x + spike.width, spike.y + spike.height)
        path.close()

        canvas.drawPath(path, paint)

        // Обводка
        paint.color = Color.parseColor("#5C0000")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
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
        removeCallbacks(frameRunnable)
        platforms.clear()
        coins.clear()
        spikes.clear()
        invalidate()
        listener?.onGameOver(score, distance)
    }

    private fun setupPlayer() {
        val width = PLAYER_WIDTH_DP * density
        val height = width * PLAYER_HEIGHT_RATIO
        val startX = viewWidth * 0.2f
        val startY = viewHeight * 0.6f

        player = Player(
            x = startX,
            y = startY,
            width = width,
            height = height
        )
        playerVelocityY = 0f
        isJumping = false
        isOnGround = true
        cameraX = 0f
    }

    private data class Player(
        var x: Float = 0f,
        var y: Float = 0f,
        var width: Float = 0f,
        var height: Float = 0f
    )

    private data class Platform(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float
    )

    private data class Coin(
        var x: Float,
        var y: Float,
        var radius: Float,
        var rotation: Float,
        var rotationSpeed: Float
    )

    private data class Spike(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float
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

        private const val COIN_SCORE = 10
        private const val DISTANCE_MULTIPLIER = 0.01f

        private const val GRAVITY = 1200f
        private const val JUMP_VELOCITY = -600f
        private const val MAX_FALL_SPEED = 800f

        private const val PLATFORM_HEIGHT = 30f
        private const val PLATFORM_MIN_WIDTH = 120f
        private const val PLATFORM_MAX_WIDTH = 250f
        private const val PLATFORM_MIN_GAP = 150f
        private const val PLATFORM_MAX_GAP = 300f
        private const val PLATFORM_COLLISION_MARGIN = 10f

        private const val COIN_RADIUS = 20f
        private const val COIN_SPAWN_CHANCE = 0.4f

        private const val SPIKE_WIDTH = 30f
        private const val SPIKE_HEIGHT = 25f
        private const val SPIKE_SPAWN_CHANCE = 0.3f

        private const val COIN_COLLECTION_PARTICLES = 6
        private const val PARTICLE_VELOCITY = 150f
        private const val PARTICLE_VELOCITY_MIN = 30f

        private const val PLAYER_WIDTH_DP = 40f
        private const val PLAYER_HEIGHT_RATIO = 1.2f
        private const val PLAYER_FORWARD_SPEED = 200f
    }
}

