package mikle.sam.game0

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    interface MusicController {
        fun pauseMusic()
        fun resumeMusic()
    }
    
    private var musicController: MusicController? = null
    
    fun setMusicController(controller: MusicController) {
        musicController = controller
    }
    
    fun isGamePaused(): Boolean {
        return isPaused
    }
    
    fun isGameOver(): Boolean {
        return gameOver
    }

    private var startX = 0f
    private var startY = 0f
    private val paint = Paint()
    private var gameLoopThread: Thread? = null
    private var isRunning = false
    private var isPaused = false
    private lateinit var player: Player
    private val obstacles = CopyOnWriteArrayList<Obstacle>()
    private var laneCount = 3
    private var initialLaneCount = 3
    private var spawnFrequency = 33
    private lateinit var lanePositions: List<Float>
    private var frameCount = 0
    private var score = 0
    private var bestScore = 0
    private var oldLaneCount = 0
    private var isAnimatingLaneChange = false
    private var laneChangeStartTs: Long = 0
    private val laneChangeAnimationDuration = 1500L
    private var isSpawningPaused = false
    private var increasingLanes = true
    private var speedBoost = 0f
    private val scorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
    }
    private val bestScorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }
    private val gameOverPaint = Paint().apply {
        color = Color.BLACK
        textSize = 100f
        textAlign = Paint.Align.CENTER
    }
    private val lanePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 10f
    }
    private val pausePaint = Paint().apply {
        color = Color.BLACK
        textSize = 100f
        textAlign = Paint.Align.CENTER
    }
    private val playIconPath = Path()
    private var gameOver = false
    private var slowdownActive = false
    private var slowdownTimer = 0L
    private var speedupActive = false
    private var speedupTimer = 0L
    private var timedGameMode = false
    private var gameStartTime = 0L
    private var lastLaneAddTime = 0L
    private var pauseStartTime = 0L
    private var lastSpawnEmptyLanes = setOf<Int>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
    private var vibrator: Vibrator? = null
    private var soundPool: SoundPool? = null
    private var collisionSoundId: Int = 0
    private var isSoundLoaded = false


    init {
        paint.color = Color.RED
        bestScore = sharedPreferences.getInt("bestScore", 0)
        if (!isInEditMode) {
            val typeface = resources.getFont(R.font.custom_font)
            scorePaint.typeface = typeface
            gameOverPaint.typeface = typeface
            pausePaint.typeface = typeface
            bestScorePaint.typeface = typeface
            
            // Initialize vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            // Initialize sound pool for collision sound
            initializeSoundPool()
        }
    }
    
    private fun initializeSoundPool() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                soundPool = SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                soundPool = SoundPool(1, android.media.AudioManager.STREAM_MUSIC, 0)
            }
            
            // Set up load complete listener to know when sound is ready
            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == collisionSoundId) {
                    isSoundLoaded = true
                }
            }
            
            // Load collision sound
            // Place your sound file (e.g., collision_sound.ogg) in res/raw/ folder
            // and name it collision_sound (lowercase, no spaces, no special chars except underscore)
            val soundResourceId = resources.getIdentifier("collision_sound", "raw", context.packageName)
            if (soundResourceId != 0) {
                collisionSoundId = soundPool?.load(context, soundResourceId, 1) ?: 0
            }
        } catch (e: Exception) {
            // Sound file not found or error loading - continue without sound
            e.printStackTrace()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Define lane positions based on screen width
        updateLanePositions(w)
        player = Player(h, lanePositions)
    }

    fun setGameParameters(lanes: Int, frequency: Int) {
        laneCount = lanes
        initialLaneCount = lanes
        spawnFrequency = frequency
    }

    fun setTimedGameMode() {
        timedGameMode = true
        initialLaneCount = 3
        laneCount = 3
    }

    fun resume() {
        isRunning = true
        if (gameStartTime == 0L) {
            gameStartTime = System.currentTimeMillis()
            lastLaneAddTime = gameStartTime
        }
        gameLoopThread = Thread {
            while (isRunning) {
                if (!isPaused) {
                    update()
                }
                postInvalidate() // Redraw the view
                Thread.sleep(16) // ~60 FPS
            }
        }
        gameLoopThread?.start()
    }

    fun pause() {
        isRunning = false
        gameLoopThread?.join()
    }

    private fun update() {
        // Game logic will go here
        frameCount++
        // Не спавним новые препятствия во время замедления, чтобы они не наслаивались
        if (!slowdownActive && frameCount % spawnFrequency == 0 && !isSpawningPaused && !isAnimatingLaneChange) {
            spawnObstacle()
        }

        if (::player.isInitialized) {
            player.update()
        }
        obstacles.forEach { 
            it.update()
            // Обновляем позиции полос для препятствий, которые меняют полосы
            if (it.type == ObstacleType.LANE_CHANGER) {
                it.updateLanePositions(lanePositions)
            }
        }
        obstacles.removeAll { it.rect.top > height }
        checkCollisions()

        if (slowdownActive && System.currentTimeMillis() - slowdownTimer > 2000) {
            restoreObstacleSpeeds()
            slowdownActive = false
        }
        if (speedupActive && System.currentTimeMillis() - speedupTimer > 2000) {
            restoreObstacleSpeeds()
            speedupActive = false
        }

        val timeSinceLastAdd = System.currentTimeMillis() - lastLaneAddTime
        if (timedGameMode && timeSinceLastAdd > 15000) {
            if (!isAnimatingLaneChange) {
                if (laneCount == 7) {
                    increasingLanes = false
                } else if (laneCount == 3) {
                    increasingLanes = true
                }
                oldLaneCount = laneCount
                if (increasingLanes) {
                    laneCount++
                } else {
                    laneCount--
                    speedBoost += 0.5f
                }
                if (isRunning) {
                    score++
                }
                isAnimatingLaneChange = true
                laneChangeStartTs = System.currentTimeMillis()
                lastLaneAddTime = System.currentTimeMillis()
            }
        } else if (timedGameMode && timeSinceLastAdd > 11000) {
            isSpawningPaused = true
        }
    }

    private fun checkCollisions() {
        obstacles.forEach {
            val obstacleWidth = it.rect.width()
            val collisionBox = RectF(
                it.rect.left + obstacleWidth * 0.25f,
                it.rect.bottom - it.rect.height() * 0.3f,
                it.rect.right - obstacleWidth * 0.25f,
                it.rect.bottom - it.rect.height() * 0.2f
            )
            if (RectF.intersects(player.rect, collisionBox)) {
                gameOver = true
                isAnimatingLaneChange = false
                if (score > bestScore) {
                    bestScore = score
                    sharedPreferences.edit().putInt("bestScore", bestScore).apply()
                }
                // Vibrate on collision
                vibrateOnCollision()
                // Play collision sound
                playCollisionSound()
                // Pause music on collision
                musicController?.pauseMusic()
                postInvalidate()
                pause()
            }
        }
    }

    private fun getUnlockedObstacleTypes(): List<ObstacleType> {
        // Постепенная разблокировка типов препятствий на основе очков
        val unlocked = mutableListOf(ObstacleType.NORMAL) // Всегда доступны обычные
        
        // Используем if для накопления - каждый тип разблокируется и остается доступным
        if (score >= 5) unlocked.add(ObstacleType.FAST)      // После 5 очков
        if (score >= 10) unlocked.add(ObstacleType.SLOW)      // После 10 очков
        if (score >= 15) unlocked.add(ObstacleType.SMALL)     // После 15 очков
        if (score >= 20) unlocked.add(ObstacleType.BIG)       // После 20 очков
        if (score >= 25) unlocked.add(ObstacleType.LANE_CHANGER) // После 25 очков
        
        return unlocked
    }
    
    private fun selectRandomObstacleType(): ObstacleType {
        val unlockedTypes = getUnlockedObstacleTypes()
        
        // Если разблокирован только NORMAL, возвращаем его
        if (unlockedTypes.size == 1) {
            return ObstacleType.NORMAL
        }
        
        // Вероятности появления разных типов препятствий (только разблокированных)
        val rand = (1..100).random()
        val normalWeight = 40
        val fastWeight = 20
        val slowWeight = 15
        val smallWeight = 10
        val bigWeight = 7
        val laneChangerWeight = 8
        
        var currentWeight = 0
        val totalWeight = unlockedTypes.sumOf { type ->
            when (type) {
                ObstacleType.NORMAL -> normalWeight
                ObstacleType.FAST -> fastWeight
                ObstacleType.SLOW -> slowWeight
                ObstacleType.SMALL -> smallWeight
                ObstacleType.BIG -> bigWeight
                ObstacleType.LANE_CHANGER -> laneChangerWeight
            }
        }
        
        // Нормализуем веса относительно доступных типов
        val normalizedRand = (rand * totalWeight) / 100
        
        currentWeight = 0
        for (type in unlockedTypes) {
            val typeWeight = when (type) {
                ObstacleType.NORMAL -> normalWeight
                ObstacleType.FAST -> fastWeight
                ObstacleType.SLOW -> slowWeight
                ObstacleType.SMALL -> smallWeight
                ObstacleType.BIG -> bigWeight
                ObstacleType.LANE_CHANGER -> laneChangerWeight
            }
            currentWeight += typeWeight
            if (normalizedRand <= currentWeight) {
                return type
            }
        }
        
        // Fallback на первый доступный тип
        return unlockedTypes.first()
    }
    
    private fun spawnObstacle() {
        val lanesToSpawnIn = lastSpawnEmptyLanes.toMutableSet()
        val minObstacles = lastSpawnEmptyLanes.size.coerceAtLeast(1)
        val maxObstacles = (laneCount - 1).coerceAtLeast(minObstacles)

        val numberOfObstacles = if (minObstacles >= maxObstacles) {
            maxObstacles
        } else {
            (minObstacles..maxObstacles).random()
        }

        val remainingNeeded = numberOfObstacles - lanesToSpawnIn.size
        if (remainingNeeded > 0) {
            val availableForRandom = (0 until laneCount).toSet() - lanesToSpawnIn
            lanesToSpawnIn.addAll(availableForRandom.shuffled().take(remainingNeeded))
        }

        // Определяем, будет ли одно специальное препятствие (не NORMAL)
        val hasSpecialObstacle = lanesToSpawnIn.size > 1 && (0..100).random() < 30 // 30% вероятность специального препятствия
        var specialObstacleSpawned = false
        
        lanesToSpawnIn.forEach { lane ->
            if (lane < lanePositions.size) {
            val x = lanePositions[lane]
            // Выбираем тип препятствия
            val type = if (hasSpecialObstacle && !specialObstacleSpawned) {
                // Первое препятствие может быть специальным
                specialObstacleSpawned = true
                selectRandomObstacleType()
            } else {
                // Остальные всегда NORMAL
                ObstacleType.NORMAL
            }
            val obstacle = Obstacle(x, 0f, type, speedBoost, 
                if (type == ObstacleType.LANE_CHANGER) lanePositions else null)

            if (slowdownActive) {
                obstacle.slowDown()
            } else if (speedupActive) {
                obstacle.speedUp()
            }
            obstacles.add(obstacle)
            }
        }

        lastSpawnEmptyLanes = (0 until laneCount).toSet() - lanesToSpawnIn
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        if (isAnimatingLaneChange && oldLaneCount > 0) {
            val elapsedTime = System.currentTimeMillis() - laneChangeStartTs
            var progress = (elapsedTime.toFloat() / laneChangeAnimationDuration).coerceAtMost(1f)
            // Apply ease-in-out interpolation
            progress = 0.5f - 0.5f * Math.cos(progress * Math.PI).toFloat()

            val M = laneCount
            val N = oldLaneCount

            val newLanePos = calculateLanePositions(width, M)
            val oldLanePos = calculateLanePositions(width, N)
            val interpolatedLanePos = newLanePos.map { newPos ->
                // Оптимизация: используем простой поиск ближайшего вместо minByOrNull
                var closestOldPos = oldLanePos[0]
                var minDist = abs(oldLanePos[0] - newPos)
                for (i in 1 until oldLanePos.size) {
                    val dist = abs(oldLanePos[i] - newPos)
                    if (dist < minDist) {
                        minDist = dist
                        closestOldPos = oldLanePos[i]
                    }
                }
                closestOldPos + (newPos - closestOldPos) * progress
            }
            player.updateLanePositions(interpolatedLanePos)

            // Кэшируем вычисления линий
            val oldLines = if (N > 1) {
                (0 until N - 1).map { j ->
                    (oldLanePos[j] + oldLanePos[j + 1]) / 2
                }
            } else {
                emptyList()
            }
            val newLines = if (M > 1) {
                (0 until M - 1).map { j ->
                    (newLanePos[j] + newLanePos[j + 1]) / 2
                }
            } else {
                emptyList()
            }
            newLines.forEachIndexed { index, newLine ->
                // Оптимизация: используем простой поиск ближайшего
                val closestOldLine = if (oldLines.isNotEmpty()) {
                    var closest = oldLines[0]
                    var minDist = abs(oldLines[0] - newLine)
                    for (i in 1 until oldLines.size) {
                        val dist = abs(oldLines[i] - newLine)
                        if (dist < minDist) {
                            minDist = dist
                            closest = oldLines[i]
                        }
                    }
                    closest
                } else {
                    width / 2f
                }
                val currentPos = closestOldLine + (newLine - closestOldLine) * progress
                canvas.drawLine(currentPos, 0f, currentPos, height.toFloat(), lanePaint)
            }

            if (progress == 1f) {
                isAnimatingLaneChange = false
                updateLanePositions(width)
                player.updateLanePositions(lanePositions)
                // Обновляем позиции полос для всех препятствий, которые меняют полосы
                obstacles.forEach { obstacle ->
                    if (obstacle.type == ObstacleType.LANE_CHANGER) {
                        obstacle.updateLanePositions(lanePositions)
                    }
                }
                lastSpawnEmptyLanes = setOf() // Reset for new lane count
                isSpawningPaused = false
            }
        } else {
            for (i in 0 until lanePositions.size - 1) {
                val left = lanePositions[i] + (lanePositions[i+1] - lanePositions[i]) / 2
                canvas.drawLine(left, 0f, left, height.toFloat(), lanePaint)
            }
        }
        if (::player.isInitialized) {
            player.draw(canvas)
        }
        obstacles.forEach { it.draw(canvas) }
        canvas.drawText("Score: $score", 50f, 100f, scorePaint)
        canvas.drawText("Best: $bestScore", 50f, 160f, scorePaint)
        if (isPaused) {
            playIconPath.reset()
            playIconPath.moveTo(width - 100f, 50f)
            playIconPath.lineTo(width - 100f, 110f)
            playIconPath.lineTo(width - 40f, 80f)
            playIconPath.close()
            canvas.drawPath(playIconPath, lanePaint)
        } else {
            canvas.drawRect(width - 120f, 50f, width - 90f, 110f, lanePaint)
            canvas.drawRect(width - 70f, 50f, width - 40f, 110f, lanePaint)
        }
        if (gameOver) {
            canvas.drawText("Game Over", width / 2f, height / 2f, gameOverPaint)
            canvas.drawText("Best Score: $bestScore", width / 2f, height / 2f + 120, bestScorePaint)
        } else if (isPaused) {
            canvas.drawText("Paused", width / 2f, height / 2f, pausePaint)
        }
    }

    private fun restart() {
        obstacles.clear()
        score = 0
        gameOver = false
        slowdownActive = false
        speedupActive = false
        isPaused = false
        isSpawningPaused = false
        lastSpawnEmptyLanes = setOf()
        if (timedGameMode) {
            laneCount = 3
            updateLanePositions(width)
            player.updateLanePositions(lanePositions)
            gameStartTime = 0L
            lastLaneAddTime = 0L
        }
        player.reset()
        musicController?.resumeMusic()
        resume()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (gameOver) {
                    restart()
                } else {
                    if (event.y < 150 && event.x > width - 150) {
                        // Toggle pause state and adjust timers
                        isPaused = !isPaused
                        if (isPaused) {
                            // Game is being paused
                            pauseStartTime = System.currentTimeMillis()
                            musicController?.pauseMusic()
                        } else {
                            // Game is being resumed
                            val pauseDuration = System.currentTimeMillis() - pauseStartTime
                            gameStartTime += pauseDuration
                            lastLaneAddTime += pauseDuration
                            if (slowdownActive) {
                                slowdownTimer += pauseDuration
                            }
                            if (speedupActive) {
                                speedupTimer += pauseDuration
                            }
                            if (isAnimatingLaneChange) {
                                laneChangeStartTs += pauseDuration
                            }
                            musicController?.resumeMusic()
                        }
                    } else {
                        startX = event.x
                        startY = event.y
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!gameOver && !isPaused) {
                    val endX = event.x
                    val endY = event.y
                    val deltaX = endX - startX
                    val deltaY = endY - startY
                    if (abs(deltaX) > abs(deltaY)) {
                        if (deltaX > 100) {
                            player.moveRight()
                        } else if (deltaX < -100) {
                            player.moveLeft()
                        }
                    } else {
                        if (deltaY > 100) {
                            slowDownObstacles()
                        } else if (deltaY < -100) {
                            speedUpObstacles()
                        }
                    }
                }
            }
        }
        return true
    }

    private fun slowDownObstacles() {
        if (!slowdownActive) {
            obstacles.forEach { it.slowDown() }
            slowdownActive = true
            slowdownTimer = System.currentTimeMillis()
        }
    }

    private fun speedUpObstacles() {
        if (!speedupActive) {
            obstacles.forEach { it.speedUp() }
            speedupActive = true
            speedupTimer = System.currentTimeMillis()
        }
    }

    private fun restoreObstacleSpeeds() {
        obstacles.forEach { it.restoreSpeed() }
    }

    private fun updateLanePositions(w: Int) {
        lanePositions = calculateLanePositions(w, laneCount)
    }

    private fun calculateLanePositions(w: Int, count: Int): List<Float> {
        return when (count) {
            2 -> listOf(w * 0.33f, w * 0.66f)
            3 -> listOf(w * 0.25f, w * 0.5f, w * 0.75f)
            else -> (0 until count).map { i -> (i + 0.5f) * w / count }
        }
    }
    
    private fun vibrateOnCollision() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect for API 26+
                val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                vib.vibrate(vibrationEffect)
            } else {
                // Fallback for older versions
                @Suppress("DEPRECATION")
                vib.vibrate(100)
            }
        }
    }
    
    private fun playCollisionSound() {
        try {
            soundPool?.let { pool ->
                if (collisionSoundId != 0) {
                    // Play sound at lower volume (0.4 = 40% volume)
                    pool.play(collisionSoundId, 0.4f, 0.4f, 1, 0, 1f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Release sound pool resources
        try {
            soundPool?.release()
            soundPool = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}