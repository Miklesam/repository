package mikle.sam.bubblepop.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ShooterGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onScoreChanged(score: Int)
        fun onLivesChanged(lives: Int)
        fun onGameOver(finalScore: Int)
        fun onLevelChanged(level: Int)
        fun onComboChanged(combo: Int)
    }

    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val enemyBullets = mutableListOf<Bullet>()
    private val explosions = mutableListOf<Explosion>()
    private val powerUps = mutableListOf<PowerUp>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playerShape = RectF()
    private val enemyBodyRect = RectF()
    private val enemyEngineRect = RectF()
    private val enemyPath = Path()
    private val random = Random(System.currentTimeMillis())

    private val density = resources.displayMetrics.density
    private val minEnemySpeedPx = MIN_ENEMY_SPEED_DP * density
    private val maxEnemySpeedPx = MAX_ENEMY_SPEED_DP * density
    private val bulletSpeedPx = BULLET_SPEED_DP * density
    private val playerMoveSpeedPx = PLAYER_MOVE_SPEED_DP * density
    private val playerPaddingPx = PLAYER_BOTTOM_PADDING_DP * density
    private val enemyColors = intArrayOf(
        Color.parseColor("#FFFFFF"), // Белый
        Color.parseColor("#E0E0E0"), // Светло-серый
        Color.parseColor("#B0B0B0"), // Серый
        Color.parseColor("#808080"), // Средне-серый
        Color.parseColor("#404040")  // Темно-серый
    )

    private var listener: Listener? = null

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var lastFrameTime = 0L
    private var lastSpawnTime = 0L
    private var lastShotTime = 0L
    private var gameStartTime = 0L
    private var running = false
    private var gameHasStarted = false

    private var score = 0
    private var lives = START_LIVES
    private var level = 1
    private var combo = 0
    private var lastComboTime = 0L
    private var comboMultiplier = 1

    // Power-up states
    private var rapidFireActive = false
    private var rapidFireEndTime = 0L
    private var slowMotionActive = false
    private var slowMotionEndTime = 0L
    private var shieldActive = false
    private var shieldEndTime = 0L
    private var lastPowerUpSpawnTime = 0L

    private var player = Player()
    private var targetPlayerX = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

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
        enemies.clear()
        bullets.clear()
        enemyBullets.clear()
        explosions.clear()
        powerUps.clear()
        score = 0
        lives = START_LIVES
        level = 1
        combo = 0
        comboMultiplier = 1
        rapidFireActive = false
        slowMotionActive = false
        shieldActive = false
        listener?.onScoreChanged(score)
        listener?.onLivesChanged(lives)
        listener?.onLevelChanged(level)
        listener?.onComboChanged(combo)

        targetPlayerX = viewWidth / 2f
        player.centerX = targetPlayerX

        gameHasStarted = true
        running = true
        val now = SystemClock.elapsedRealtime()
        gameStartTime = now
        lastFrameTime = now
        lastSpawnTime = now
        lastShotTime = now
        lastComboTime = now
        lastPowerUpSpawnTime = now
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
        lastSpawnTime = lastFrameTime
        lastShotTime = lastFrameTime
        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    fun stopGame() {
        running = false
        gameHasStarted = false
        removeCallbacks(frameRunnable)
        enemies.clear()
        bullets.clear()
        enemyBullets.clear()
        explosions.clear()
        powerUps.clear()
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

        // draw player
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        playerShape.set(
            player.centerX - player.width / 2f,
            player.y,
            player.centerX + player.width / 2f,
            player.y + player.height
        )
        canvas.drawRoundRect(playerShape, player.height / 3f, player.height / 3f, paint)

        // muzzle
        val turretWidth = player.width * 0.2f
        val turretHeight = player.height * 0.6f
        paint.color = Color.parseColor("#404040")
        canvas.drawRect(
            player.centerX - turretWidth / 2f,
            player.y - turretHeight,
            player.centerX + turretWidth / 2f,
            player.y,
            paint
        )

        // draw bullets
        paint.color = Color.WHITE
        for (bullet in bullets) {
            canvas.drawCircle(bullet.x, bullet.y, bullet.radius, paint)
        }

        // draw enemies
        for (enemy in enemies) {
            drawEnemy(canvas, enemy)
        }

        // draw explosions
        for (explosion in explosions) {
            drawExplosion(canvas, explosion)
        }

        // draw enemy bullets
        paint.color = Color.parseColor("#808080")
        for (bullet in enemyBullets) {
            canvas.drawCircle(bullet.x, bullet.y, bullet.radius, paint)
        }

        // draw power-ups
        for (powerUp in powerUps) {
            drawPowerUp(canvas, powerUp)
        }

        // draw shield if active
        if (shieldActive) {
            drawShield(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                updateTargetPosition(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex != -1) {
                    updateTargetPosition(event.getX(pointerIndex))
                } else {
                    // pointer lost, pick the first one
                    activePointerId = event.getPointerId(0)
                    updateTargetPosition(event.getX(0))
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                activePointerId = event.getPointerId(index)
                updateTargetPosition(event.getX(index))
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun updateTargetPosition(x: Float) {
        targetPlayerX = clamp(x, player.width / 2f, viewWidth - player.width / 2f)
    }

    private fun updateGame() {
        val now = SystemClock.elapsedRealtime()
        val deltaMillis = (now - lastFrameTime).coerceAtMost(MAX_FRAME_DELTA)
        lastFrameTime = now
        val deltaSeconds = deltaMillis / 1000f

        updateDifficulty(now)
        updateCombo(now)
        updatePowerUps(now, deltaSeconds)
        checkPowerUpExpiration(now)

        movePlayer(deltaSeconds)
        updateBullets(deltaSeconds)
        updateEnemyBullets(deltaSeconds)
        updateEnemies(deltaSeconds)
        updateExplosions(deltaSeconds)
        handleCollisions()
        handlePowerUpCollisions()

        val currentSpawnInterval = getCurrentSpawnInterval()
        if (now - lastSpawnTime >= currentSpawnInterval && enemies.size < MAX_ENEMIES) {
            spawnEnemy()
            lastSpawnTime = now
        }

        val currentShotInterval = getCurrentShotInterval()
        if (now - lastShotTime >= currentShotInterval) {
            fireBullet()
            lastShotTime = now
        }

        // Spawn power-ups occasionally
        if (now - lastPowerUpSpawnTime >= POWER_UP_SPAWN_INTERVAL_MS && powerUps.size < MAX_POWER_UPS) {
            spawnPowerUp()
            lastPowerUpSpawnTime = now
        }
    }

    private fun movePlayer(deltaSeconds: Float) {
        val distance = targetPlayerX - player.centerX
        if (abs(distance) < playerMoveSpeedPx * deltaSeconds) {
            player.centerX = targetPlayerX
        } else {
            val direction = if (distance > 0) 1 else -1
            player.centerX += direction * playerMoveSpeedPx * deltaSeconds
        }
    }

    private fun updateBullets(deltaSeconds: Float) {
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.y -= bullet.speed * deltaSeconds
            if (bullet.y + bullet.radius < 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateEnemyBullets(deltaSeconds: Float) {
        val iterator = enemyBullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.y += bullet.speed * deltaSeconds
            if (bullet.y - bullet.radius > viewHeight) {
                iterator.remove()
            } else if (bullet.y + bullet.radius >= player.y &&
                abs(bullet.x - player.centerX) <= (bullet.radius + player.width / 2f)
            ) {
                // Hit player
                iterator.remove()
                if (!shieldActive) {
                    loseLife()
                }
            }
        }
    }

    private fun updateEnemies(deltaSeconds: Float) {
        val speedMultiplier = if (slowMotionActive) 0.5f else 1f
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            
            // Update movement pattern
            when (enemy.movementPattern) {
                MovementPattern.STRAIGHT -> {
                    enemy.y += enemy.speed * deltaSeconds * speedMultiplier
                }
                MovementPattern.ZIGZAG -> {
                    enemy.y += enemy.speed * deltaSeconds * speedMultiplier
                    val waveOffset = kotlin.math.sin((enemy.y / 50f) * 2f) * 30f
                    enemy.x = enemy.startX + waveOffset
                }
                MovementPattern.WAVE -> {
                    enemy.y += enemy.speed * deltaSeconds * speedMultiplier
                    val waveOffset = kotlin.math.sin((enemy.y / 40f) * 3f) * 40f
                    enemy.x = enemy.startX + waveOffset
                }
            }
            
            if (enemy.y - enemy.radius > viewHeight) {
                iterator.remove()
                if (!shieldActive) {
                    loseLife()
                }
                if (!running) return
            } else if (enemy.y + enemy.radius >= player.y &&
                abs(enemy.x - player.centerX) <= (enemy.radius + player.width / 2f)
            ) {
                iterator.remove()
                if (!shieldActive) {
                    loseLife()
                }
                if (!running) return
            }
            
            // Enemy shooting
            if (enemy.canShoot && enemy.y > 100f && enemy.y < viewHeight * 0.7f) {
                val now = SystemClock.elapsedRealtime()
                if (now - enemy.lastShotTime >= ENEMY_SHOOT_INTERVAL_MS) {
                    fireEnemyBullet(enemy)
                    enemy.lastShotTime = now
                }
            }
        }
    }

    private fun handleCollisions() {
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            val enemyIterator = enemies.iterator()
            while (enemyIterator.hasNext()) {
                val enemy = enemyIterator.next()
                if (circleCollision(
                        bullet.x,
                        bullet.y,
                        bullet.radius,
                        enemy.x,
                        enemy.y,
                        enemy.radius
                    )
                ) {
                    bulletIterator.remove()
                    enemy.hits--
                    
                    if (enemy.hits <= 0) {
                        enemyIterator.remove()
                        // Create explosion effect
                        createExplosion(enemy.x, enemy.y, enemy.color)
                        
                        // Update combo
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastComboTime < COMBO_TIMEOUT_MS) {
                            combo++
                        } else {
                            combo = 1
                        }
                        lastComboTime = now
                        comboMultiplier = min(combo / COMBO_MULTIPLIER_STEP + 1, MAX_COMBO_MULTIPLIER)
                        
                        // Calculate score with combo multiplier
                        val baseScore = SCORE_PER_ENEMY * enemy.type.scoreMultiplier
                        val finalScore = baseScore * comboMultiplier
                        score += finalScore
                        
                        listener?.onScoreChanged(score)
                        listener?.onComboChanged(combo)
                    }
                    break
                }
            }
        }
    }

    private fun handlePowerUpCollisions() {
        val powerUpIterator = powerUps.iterator()
        while (powerUpIterator.hasNext()) {
            val powerUp = powerUpIterator.next()
            val dx = powerUp.x - player.centerX
            val dy = powerUp.y - player.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance <= powerUp.radius + player.width / 2f) {
                powerUpIterator.remove()
                activatePowerUp(powerUp.type)
            }
        }
    }

    private fun circleCollision(
        x1: Float,
        y1: Float,
        r1: Float,
        x2: Float,
        y2: Float,
        r2: Float
    ): Boolean {
        val dx = x1 - x2
        val dy = y1 - y2
        val radiusSum = r1 + r2
        return dx * dx + dy * dy <= radiusSum * radiusSum
    }

    private fun spawnEnemy() {
        if (viewWidth <= 0f || viewHeight <= 0f) return

        // Determine enemy type based on level and randomness
        val enemyType = determineEnemyType()
        val baseRadius = min(viewWidth, viewHeight) * 0.08f
        val radius = when (enemyType) {
            EnemyType.SMALL -> baseRadius * 0.5f
            EnemyType.NORMAL -> baseRadius * 0.7f
            EnemyType.LARGE -> baseRadius * 1.2f
            EnemyType.FAST -> baseRadius * 0.6f
            EnemyType.BOSS -> baseRadius * 2f
        }
        
        val startX = random.nextFloat() * (viewWidth - 2 * radius) + radius
        val baseSpeed = random.nextFloat() * (maxEnemySpeedPx - minEnemySpeedPx) + minEnemySpeedPx
        val speed = when (enemyType) {
            EnemyType.FAST -> baseSpeed * 1.6f
            EnemyType.LARGE -> baseSpeed * 0.7f
            EnemyType.BOSS -> baseSpeed * 0.5f
            else -> baseSpeed
        }
        val speedMultiplier = 1f + (level - 1) * DIFFICULTY_SPEED_INCREASE_PER_LEVEL
        val finalSpeed = speed * speedMultiplier
        
        val color = enemyColors[random.nextInt(enemyColors.size)]
        
        // Determine movement pattern
        val movementPattern = when {
            level >= 4 && random.nextFloat() < 0.3f -> MovementPattern.ZIGZAG
            level >= 6 && random.nextFloat() < 0.2f -> MovementPattern.WAVE
            else -> MovementPattern.STRAIGHT
        }
        
        // Bosses and some enemies can shoot
        val canShoot = enemyType == EnemyType.BOSS || (level >= 5 && random.nextFloat() < 0.15f)

        val now = SystemClock.elapsedRealtime()
        enemies.add(
            Enemy(
                x = startX,
                y = -radius,
                radius = radius,
                speed = finalSpeed,
                color = color,
                type = enemyType,
                movementPattern = movementPattern,
                startX = startX,
                canShoot = canShoot,
                lastShotTime = now - ENEMY_SHOOT_INTERVAL_MS, // Allow immediate shot
                hits = if (enemyType == EnemyType.BOSS) 3 else 1
            )
        )
    }
    
    private fun determineEnemyType(): EnemyType {
        val rand = random.nextFloat()
        return when {
            level >= 7 && rand < 0.1f -> EnemyType.BOSS
            level >= 5 && rand < 0.15f -> EnemyType.FAST
            level >= 3 && rand < 0.2f -> EnemyType.LARGE
            rand < 0.25f -> EnemyType.SMALL
            else -> EnemyType.NORMAL
        }
    }

    private fun fireBullet() {
        val bulletRadius = BULLET_RADIUS_DP * density
        val bulletY = player.y - player.height * 0.2f
        bullets.add(
            Bullet(
                x = player.centerX,
                y = bulletY,
                radius = bulletRadius,
                speed = bulletSpeedPx
            )
        )
    }

    private fun loseLife() {
        if (!running) return

        lives -= 1
        combo = 0
        comboMultiplier = 1
        listener?.onLivesChanged(lives)
        listener?.onComboChanged(combo)
        if (lives <= 0) {
            finishGame()
        }
    }
    
    private fun updateDifficulty(now: Long) {
        val gameDuration = now - gameStartTime
        val newLevel = (gameDuration / LEVEL_DURATION_MS).toInt() + 1
        if (newLevel > level && newLevel <= MAX_LEVEL) {
            level = newLevel
            listener?.onLevelChanged(level)
        }
    }
    
    private fun updateCombo(now: Long) {
        if (combo > 0 && now - lastComboTime >= COMBO_TIMEOUT_MS) {
            combo = 0
            comboMultiplier = 1
            listener?.onComboChanged(combo)
        }
    }
    
    private fun getCurrentSpawnInterval(): Long {
        val baseInterval = SPAWN_INTERVAL_MS
        val reduction = (level - 1) * DIFFICULTY_SPAWN_REDUCTION_PER_LEVEL
        return (baseInterval - reduction).coerceAtLeast(MIN_SPAWN_INTERVAL_MS)
    }
    
    private fun getCurrentShotInterval(): Long {
        return if (rapidFireActive) {
            SHOT_INTERVAL_MS / 3
        } else {
            SHOT_INTERVAL_MS
        }
    }

    private fun spawnPowerUp() {
        if (viewWidth <= 0f || viewHeight <= 0f) return
        
        val powerUpType = PowerUpType.values()[random.nextInt(PowerUpType.values().size)]
        val radius = POWER_UP_RADIUS_DP * density
        val x = random.nextFloat() * (viewWidth - 2 * radius) + radius
        val y = -radius
        
        powerUps.add(
            PowerUp(
                x = x,
                y = y,
                radius = radius,
                speed = minEnemySpeedPx * 0.7f,
                type = powerUpType
            )
        )
    }

    private fun updatePowerUps(now: Long, deltaSeconds: Float) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.y += powerUp.speed * deltaSeconds
            if (powerUp.y - powerUp.radius > viewHeight) {
                iterator.remove()
            }
        }
    }

    private fun activatePowerUp(type: PowerUpType) {
        val now = SystemClock.elapsedRealtime()
        when (type) {
            PowerUpType.RAPID_FIRE -> {
                rapidFireActive = true
                rapidFireEndTime = now + POWER_UP_DURATION_MS
            }
            PowerUpType.SLOW_MOTION -> {
                slowMotionActive = true
                slowMotionEndTime = now + POWER_UP_DURATION_MS
            }
            PowerUpType.SHIELD -> {
                shieldActive = true
                shieldEndTime = now + POWER_UP_DURATION_MS
            }
            PowerUpType.EXTRA_LIFE -> {
                lives++
                listener?.onLivesChanged(lives)
            }
        }
    }

    private fun checkPowerUpExpiration(now: Long) {
        if (rapidFireActive && now >= rapidFireEndTime) {
            rapidFireActive = false
        }
        if (slowMotionActive && now >= slowMotionEndTime) {
            slowMotionActive = false
        }
        if (shieldActive && now >= shieldEndTime) {
            shieldActive = false
        }
    }

    private fun fireEnemyBullet(enemy: Enemy) {
        val bulletRadius = BULLET_RADIUS_DP * density * 0.8f
        val bulletSpeed = bulletSpeedPx * 0.6f
        enemyBullets.add(
            Bullet(
                x = enemy.x,
                y = enemy.y + enemy.radius,
                radius = bulletRadius,
                speed = bulletSpeed
            )
        )
    }

    private fun drawPowerUp(canvas: Canvas, powerUp: PowerUp) {
        paint.style = Paint.Style.FILL
        
        // Draw outer circle
        paint.color = when (powerUp.type) {
            PowerUpType.RAPID_FIRE -> Color.parseColor("#FFFFFF")
            PowerUpType.SLOW_MOTION -> Color.parseColor("#E0E0E0")
            PowerUpType.SHIELD -> Color.parseColor("#B0B0B0")
            PowerUpType.EXTRA_LIFE -> Color.parseColor("#808080")
        }
        canvas.drawCircle(powerUp.x, powerUp.y, powerUp.radius, paint)
        
        // Draw inner symbol
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = powerUp.radius * 0.2f
        when (powerUp.type) {
            PowerUpType.RAPID_FIRE -> {
                // Draw lightning bolt
                val path = Path()
                path.moveTo(powerUp.x, powerUp.y - powerUp.radius * 0.6f)
                path.lineTo(powerUp.x - powerUp.radius * 0.3f, powerUp.y)
                path.lineTo(powerUp.x, powerUp.y)
                path.lineTo(powerUp.x + powerUp.radius * 0.3f, powerUp.y + powerUp.radius * 0.6f)
                canvas.drawPath(path, paint)
            }
            PowerUpType.SLOW_MOTION -> {
                // Draw clock
                canvas.drawCircle(powerUp.x, powerUp.y, powerUp.radius * 0.5f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(powerUp.x, powerUp.y, powerUp.radius * 0.1f, paint)
            }
            PowerUpType.SHIELD -> {
                // Draw shield
                val path = Path()
                path.moveTo(powerUp.x, powerUp.y - powerUp.radius * 0.6f)
                path.lineTo(powerUp.x - powerUp.radius * 0.5f, powerUp.y)
                path.lineTo(powerUp.x, powerUp.y + powerUp.radius * 0.6f)
                path.lineTo(powerUp.x + powerUp.radius * 0.5f, powerUp.y)
                path.close()
                canvas.drawPath(path, paint)
            }
            PowerUpType.EXTRA_LIFE -> {
                // Draw plus
                paint.style = Paint.Style.STROKE
                canvas.drawLine(
                    powerUp.x - powerUp.radius * 0.4f,
                    powerUp.y,
                    powerUp.x + powerUp.radius * 0.4f,
                    powerUp.y,
                    paint
                )
                canvas.drawLine(
                    powerUp.x,
                    powerUp.y - powerUp.radius * 0.4f,
                    powerUp.x,
                    powerUp.y + powerUp.radius * 0.4f,
                    paint
                )
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawShield(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#FFFFFF")
        paint.strokeWidth = 4f
        paint.alpha = 150
        val shieldRadius = player.width * 0.7f
        canvas.drawCircle(player.centerX, player.y + player.height / 2f, shieldRadius, paint)
        paint.alpha = 255
    }
    
    private fun createExplosion(x: Float, y: Float, color: Int) {
        explosions.add(Explosion(x, y, color, SystemClock.elapsedRealtime()))
    }
    
    private fun updateExplosions(deltaSeconds: Float) {
        val now = SystemClock.elapsedRealtime()
        val iterator = explosions.iterator()
        while (iterator.hasNext()) {
            val explosion = iterator.next()
            explosion.age = now - explosion.startTime
            if (explosion.age > EXPLOSION_DURATION_MS) {
                iterator.remove()
            }
        }
    }
    
    private fun drawExplosion(canvas: Canvas, explosion: Explosion) {
        val progress = (explosion.age / EXPLOSION_DURATION_MS.toFloat()).coerceIn(0f, 1f)
        val alpha = (255 * (1f - progress)).toInt()
        val radius = explosion.baseRadius * (1f + progress * 2f)
        
        paint.color = explosion.color
        paint.alpha = alpha
        paint.style = Paint.Style.FILL
        canvas.drawCircle(explosion.x, explosion.y, radius, paint)
        
        // Draw inner bright circle
        paint.color = Color.parseColor("#E0E0E0")
        paint.alpha = (alpha * 0.6f).toInt()
        canvas.drawCircle(explosion.x, explosion.y, radius * 0.5f, paint)
        
        paint.alpha = 255
    }

    private fun finishGame() {
        running = false
        gameHasStarted = false
        removeCallbacks(frameRunnable)
        enemies.clear()
        bullets.clear()
        enemyBullets.clear()
        explosions.clear()
        powerUps.clear()
        invalidate()
        listener?.onGameOver(score)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
    }

    private fun setupPlayer() {
        val width = max(viewWidth * PLAYER_WIDTH_RATIO, MIN_PLAYER_WIDTH_DP * density)
        val height = width * PLAYER_HEIGHT_RATIO
        val y = viewHeight - height - playerPaddingPx
        player = Player(
            centerX = viewWidth / 2f,
            y = y,
            width = width,
            height = height
        )
        targetPlayerX = player.centerX
    }

    private fun clamp(value: Float, minValue: Float, maxValue: Float): Float {
        return when {
            value < minValue -> minValue
            value > maxValue -> maxValue
            else -> value
        }
    }

    private fun drawEnemy(canvas: Canvas, enemy: Enemy) {
        paint.style = Paint.Style.FILL
        val cx = enemy.x
        val cy = enemy.y
        val bodyHeight = enemy.radius * 2.6f
        val bodyWidth = enemy.radius * 1.4f
        val bodyTop = cy - bodyHeight * 0.5f
        val bodyBottom = cy + bodyHeight * 0.5f

        enemyBodyRect.set(
            cx - bodyWidth / 2f,
            bodyTop,
            cx + bodyWidth / 2f,
            bodyBottom
        )

        paint.color = enemy.color
        canvas.drawRoundRect(
            enemyBodyRect,
            enemy.radius * 0.4f,
            enemy.radius * 0.4f,
            paint
        )

        val noseHeight = enemy.radius * 1.1f
        enemyPath.reset()
        enemyPath.moveTo(cx, bodyTop - noseHeight)
        enemyPath.lineTo(cx - bodyWidth / 2.3f, bodyTop + enemy.radius * 0.15f)
        enemyPath.lineTo(cx + bodyWidth / 2.3f, bodyTop + enemy.radius * 0.15f)
        enemyPath.close()
        paint.color = lightenColor(enemy.color, 0.35f)
        canvas.drawPath(enemyPath, paint)

        val wingHeight = enemy.radius * 0.6f
        val wingWidth = bodyWidth * 1.4f
        enemyPath.reset()
        enemyPath.moveTo(cx - wingWidth / 2f, cy + wingHeight * 0.1f)
        enemyPath.lineTo(cx + wingWidth / 2f, cy + wingHeight * 0.1f)
        enemyPath.lineTo(cx, cy + wingHeight)
        enemyPath.close()
        paint.color = lightenColor(enemy.color, -0.2f)
        canvas.drawPath(enemyPath, paint)

        val engineWidth = bodyWidth * 0.55f
        val engineHeight = enemy.radius * 0.6f
        enemyEngineRect.set(
            cx - engineWidth / 2f,
            bodyBottom - engineHeight * 0.2f,
            cx + engineWidth / 2f,
            bodyBottom + engineHeight
        )
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawRoundRect(
            enemyEngineRect,
            enemy.radius * 0.2f,
            enemy.radius * 0.2f,
            paint
        )
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val clamped = factor.coerceIn(-1f, 1f)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val adjust = { c: Int ->
            if (clamped >= 0f) {
                c + ((255 - c) * clamped).toInt()
            } else {
                (c * (1 + clamped)).toInt()
            }
        }
        return Color.rgb(
            adjust(r).coerceIn(0, 255),
            adjust(g).coerceIn(0, 255),
            adjust(b).coerceIn(0, 255)
        )
    }

    private enum class EnemyType(val scoreMultiplier: Int) {
        SMALL(1),
        NORMAL(2),
        LARGE(3),
        FAST(2),
        BOSS(10)
    }
    
    private enum class MovementPattern {
        STRAIGHT,
        ZIGZAG,
        WAVE
    }
    
    private enum class PowerUpType {
        RAPID_FIRE,
        SLOW_MOTION,
        SHIELD,
        EXTRA_LIFE
    }
    
    private data class Enemy(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var color: Int,
        var type: EnemyType = EnemyType.NORMAL,
        var movementPattern: MovementPattern = MovementPattern.STRAIGHT,
        var startX: Float = 0f,
        var canShoot: Boolean = false,
        var lastShotTime: Long = 0L,
        var hits: Int = 1
    )
    
    private data class PowerUp(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var type: PowerUpType
    )
    
    private data class Explosion(
        val x: Float,
        val y: Float,
        val color: Int,
        val startTime: Long,
        var age: Long = 0L,
        val baseRadius: Float = 20f
    )

    private data class Bullet(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float
    )

    private data class Player(
        var centerX: Float = 0f,
        var y: Float = 0f,
        var width: Float = 0f,
        var height: Float = 0f
    )

    companion object {
        private const val FRAME_DELAY_MS = 16L
        private const val SPAWN_INTERVAL_MS = 750L
        private const val MIN_SPAWN_INTERVAL_MS = 300L
        private const val SHOT_INTERVAL_MS = 300L
        private const val MAX_ENEMIES = 7
        private const val START_LIVES = 3
        private const val MAX_FRAME_DELTA = 80L
        private const val SCORE_PER_ENEMY = 2
        
        // Difficulty progression
        private const val LEVEL_DURATION_MS = 15000L // 15 seconds per level
        private const val MAX_LEVEL = 10
        private const val DIFFICULTY_SPEED_INCREASE_PER_LEVEL = 0.15f
        private const val DIFFICULTY_SPAWN_REDUCTION_PER_LEVEL = 50L
        
        // Combo system
        private const val COMBO_TIMEOUT_MS = 2000L
        private const val COMBO_MULTIPLIER_STEP = 3
        private const val MAX_COMBO_MULTIPLIER = 5
        
        // Explosion effects
        private const val EXPLOSION_DURATION_MS = 400L
        
        // Power-ups
        private const val POWER_UP_SPAWN_INTERVAL_MS = 12000L
        private const val POWER_UP_DURATION_MS = 8000L
        private const val POWER_UP_RADIUS_DP = 16f
        private const val MAX_POWER_UPS = 2
        
        // Enemy shooting
        private const val ENEMY_SHOOT_INTERVAL_MS = 2000L

        private const val MIN_ENEMY_SPEED_DP = 60f
        private const val MAX_ENEMY_SPEED_DP = 140f
        private const val BULLET_SPEED_DP = 280f
        private const val PLAYER_MOVE_SPEED_DP = 260f
        private const val PLAYER_BOTTOM_PADDING_DP = 24f
        private const val BULLET_RADIUS_DP = 6f
        private const val PLAYER_WIDTH_RATIO = 0.18f
        private const val PLAYER_HEIGHT_RATIO = 0.45f
        private const val MIN_PLAYER_WIDTH_DP = 72f
    }
}

