package mikle.sam.game0

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

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
        val currentSpawnFrequency = if (slowdownActive) spawnFrequency * 4 else spawnFrequency
        if (frameCount % currentSpawnFrequency == 0 && !isSpawningPaused && !isAnimatingLaneChange) {
            spawnObstacle()
        }

        if (::player.isInitialized) {
            player.update()
        }
        obstacles.forEach { it.update() }
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
                postInvalidate()
                pause()
            }
        }
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

        lanesToSpawnIn.forEach { lane ->
            if (lane < lanePositions.size) {
            val x = lanePositions[lane]
            val type = ObstacleType.SQUARE
            val obstacle = Obstacle(x, 0f, type, speedBoost)

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
                val closestOldPos = oldLanePos.minByOrNull { Math.abs(it - newPos) } ?: (width / 2f)
                closestOldPos + (newPos - closestOldPos) * progress
            }
            player.updateLanePositions(interpolatedLanePos)

            val oldLines = (0 until N - 1).map { j ->
                val p1 = calculateLanePositions(width, N)[j]
                val p2 = calculateLanePositions(width, N)[j+1]
                p1 + (p2 - p1) / 2
            }
            val newLines = (0 until M - 1).map { j ->
                val p1 = calculateLanePositions(width, M)[j]
                val p2 = calculateLanePositions(width, M)[j+1]
                p1 + (p2 - p1) / 2
            }
            newLines.forEach { newLine ->
                val closestOldLine = oldLines.minByOrNull { Math.abs(it - newLine) } ?: (width / 2f)
                val currentPos = closestOldLine + (newLine - closestOldLine) * progress
                canvas.drawLine(currentPos, 0f, currentPos, height.toFloat(), lanePaint)
            }

            if (progress == 1f) {
                isAnimatingLaneChange = false
                updateLanePositions(width)
                player.updateLanePositions(lanePositions)
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
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
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
}