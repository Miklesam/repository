package mikle.sam.towerdefense.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import mikle.sam.towerdefense.model.*

class TowerDefenseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface GameListener {
        fun onWaveChanged(wave: Int)
        fun onGoldChanged(gold: Int)
        fun onLivesChanged(lives: Int)
        fun onGameOver(victory: Boolean)
        fun onWaveComplete()
        fun onTowerSelected(tower: Tower?)
    }

    private var listener: GameListener? = null
    private val path = Path()
    private val towers = mutableListOf<Tower>()
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val waveManager = WaveManager()

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34495E")
        style = Paint.Style.FILL
    }

    private val towerBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#95A5A6")
        style = Paint.Style.FILL
    }

    private val towerCannonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7F8C8D")
        style = Paint.Style.FILL
    }

    private val enemyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F39C12")
        style = Paint.Style.FILL
    }

    private val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.FILL
    }

    private var viewWidth = 0f
    private var viewHeight = 0f
    private var lastFrameTime = 0L
    private var isPaused = false
    private var isRunning = false
    private var gold = 100
    private var lives = 20
    private var selectedTowerType: TowerType? = null
    private var previewTowerPosition: PointF? = null
    private var selectedTower: Tower? = null
    private var gameSpeed = 1.0f // Speed multiplier (1.0 = normal, 2.0 = 2x speed)

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRunning || isPaused) return
            updateGame()
            invalidate()
            postDelayed(this, 16) // ~60 FPS
        }
    }

    fun setGameListener(listener: GameListener?) {
        this.listener = listener
    }

    fun startWave() {
        if (isPaused) return
        if (!isRunning) {
            isRunning = true
            lastFrameTime = SystemClock.elapsedRealtime()
            removeCallbacks(frameRunnable)
            post(frameRunnable)
        }
        val now = SystemClock.elapsedRealtime()
        val waveInfo = waveManager.startWave(now)
        listener?.onWaveChanged(waveInfo.waveNumber)
    }

    fun pauseGame() {
        isPaused = true
    }

    fun resumeGame() {
        isPaused = false
        if (isRunning) {
            lastFrameTime = SystemClock.elapsedRealtime()
        }
    }

    fun isPaused(): Boolean = isPaused

    fun resetGame() {
        towers.clear()
        enemies.clear()
        bullets.clear()
        gold = 100
        lives = 20
        isPaused = false
        isRunning = true
        val now = SystemClock.elapsedRealtime()
        waveManager.startWave(now)
        listener?.onGoldChanged(gold)
        listener?.onLivesChanged(lives)
        listener?.onWaveChanged(1)
        lastFrameTime = now
        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        setupPath()
        if (!isRunning) {
            resetGame()
        }
    }

    private fun setupPath() {
        // Create S-shaped path
        val waypoints = listOf(
            PointF(0f, viewHeight * 0.3f),
            PointF(viewWidth * 0.3f, viewHeight * 0.3f),
            PointF(viewWidth * 0.3f, viewHeight * 0.7f),
            PointF(viewWidth * 0.7f, viewHeight * 0.7f),
            PointF(viewWidth * 0.7f, viewHeight * 0.3f),
            PointF(viewWidth, viewHeight * 0.3f)
        )
        path.setWaypoints(waypoints)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(Color.parseColor("#2C3E50"))

        // Draw path
        drawPath(canvas)

        // Draw towers
        for (tower in towers) {
            val isSelected = tower == selectedTower
            drawTower(canvas, tower, isSelected)
        }

        // Draw preview tower
        previewTowerPosition?.let { pos ->
            selectedTowerType?.let { type ->
                drawTowerPreview(canvas, pos, type)
            }
        }

        // Draw enemies
        for (enemy in enemies) {
            drawEnemy(canvas, enemy)
        }

        // Draw bullets
        for (bullet in bullets) {
            drawBullet(canvas, bullet)
        }
    }

    private fun drawPath(canvas: Canvas) {
        val waypoints = path.getWaypoints()
        if (waypoints.size < 2) return

        val pathWidth = 80f
        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]

            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = kotlin.math.sqrt(dx * dx + dy * dy)
            val angle = kotlin.math.atan2(dy, dx)

            canvas.save()
            canvas.translate(start.x, start.y)
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
            canvas.drawRect(0f, -pathWidth / 2, length, pathWidth / 2, pathPaint)
            canvas.restore()
        }
    }

    private fun drawTower(canvas: Canvas, tower: Tower, isSelected: Boolean = false) {
        val x = tower.position.x
        val y = tower.position.y
        val radius = 30f

        // Draw range circle if selected
        if (isSelected) {
            canvas.drawCircle(x, y, tower.getRange(), rangePaint)
        }

        // Draw selection ring
        if (isSelected) {
            val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#80FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(x, y, radius + 5f, selectionPaint)
        }

        // Draw base
        canvas.drawCircle(x, y, radius, towerBasePaint)

        // Draw cannon
        if (tower.targetEnemy != null) {
            val angle = tower.getAngleToTarget(tower.targetEnemy!!.position)
            val cannonLength = radius * 1.5f
            val cannonEndX = x + kotlin.math.cos(angle) * cannonLength
            val cannonEndY = y + kotlin.math.sin(angle) * cannonLength

            val cannonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = towerCannonPaint.color
                strokeWidth = 12f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(x, y, cannonEndX.toFloat(), cannonEndY.toFloat(), cannonPaint)
        } else {
            canvas.drawCircle(x, y, radius * 0.6f, towerCannonPaint)
        }
        
        // Draw level indicator
        if (tower.level > 1) {
            val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 16f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("L${tower.level}", x, y + radius + 15, levelPaint)
        }
    }

    private fun drawTowerPreview(canvas: Canvas, position: PointF, type: TowerType) {
        val radius = 30f
        val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(position.x, position.y, radius, previewPaint)
        canvas.drawCircle(position.x, position.y, type.range, previewPaint)
    }

    private fun drawEnemy(canvas: Canvas, enemy: Enemy) {
        if (enemy.isDead) return

        enemyPaint.color = enemy.type.color
        val radius = when (enemy.type) {
            EnemyType.TANK -> 25f
            EnemyType.ELITE -> 22f
            EnemyType.BASIC -> 20f
            EnemyType.FAST -> 15f
        }
        canvas.drawCircle(enemy.position.x, enemy.position.y, radius, enemyPaint)

        // Draw health bar
        val barWidth = radius * 2
        val barHeight = 4f
        val healthPercent = enemy.health.toFloat() / enemy.type.maxHealth

        val bgPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        val healthPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
        }

        canvas.drawRect(
            enemy.position.x - barWidth / 2,
            enemy.position.y - radius - 10,
            enemy.position.x + barWidth / 2,
            enemy.position.y - radius - 10 + barHeight,
            bgPaint
        )
        canvas.drawRect(
            enemy.position.x - barWidth / 2,
            enemy.position.y - radius - 10,
            enemy.position.x - barWidth / 2 + barWidth * healthPercent,
            enemy.position.y - radius - 10 + barHeight,
            healthPaint
        )
    }

    private fun drawBullet(canvas: Canvas, bullet: Bullet) {
        if (!bullet.isActive) return
        canvas.drawCircle(bullet.position.x, bullet.position.y, 5f, bulletPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Check if clicking on existing tower
                val clickedTower = findTowerAt(x, y)
                if (clickedTower != null) {
                    selectedTower = clickedTower
                    selectedTowerType = null
                    previewTowerPosition = null
                    listener?.onTowerSelected(clickedTower)
                    invalidate()
                    return true
                }

                // Check if clicking on path (can't place tower on path)
                if (isPointOnPath(x, y)) {
                    selectedTower = null
                    selectedTowerType = null
                    previewTowerPosition = null
                    listener?.onTowerSelected(null)
                    invalidate()
                    return true
                }

                // If preview mode, place tower
                if (selectedTowerType != null) {
                    placeTower(PointF(x, y), selectedTowerType!!)
                    selectedTowerType = null
                    previewTowerPosition = null
                    selectedTower = null
                    listener?.onTowerSelected(null)
                    return true
                }

                // If clicking empty space, try to place a basic tower directly
                selectedTower = null
                val placed = placeTower(PointF(x, y), TowerType.BASIC)
                if (!placed) {
                    // If can't place (not enough gold or too close), show preview
                    selectedTowerType = TowerType.BASIC
                    previewTowerPosition = PointF(x, y)
                }
                listener?.onTowerSelected(null)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedTowerType != null) {
                    previewTowerPosition = PointF(event.x, event.y)
                    invalidate()
                }
            }
        }
        return true
    }
    
    private fun findTowerAt(x: Float, y: Float): Tower? {
        for (tower in towers) {
            val dx = x - tower.position.x
            val dy = y - tower.position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance <= 30f) {
                return tower
            }
        }
        return null
    }
    
    fun upgradeSelectedTower(): Boolean {
        val tower = selectedTower ?: return false
        if (!tower.canUpgrade()) return false
        
        val cost = tower.getUpgradeCost()
        if (gold < cost) return false
        
        gold -= cost
        tower.upgrade()
        listener?.onGoldChanged(gold)
        invalidate()
        return true
    }
    
    fun getSelectedTower(): Tower? = selectedTower
    
    fun getCurrentGold(): Int = gold
    
    fun setGameSpeed(speed: Float) {
        gameSpeed = speed.coerceIn(0.5f, 3.0f)
    }
    
    fun getGameSpeed(): Float = gameSpeed

    private fun isPointOnPath(x: Float, y: Float): Boolean {
        val pathWidth = 80f
        val waypoints = path.getWaypoints()

        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]

            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = kotlin.math.sqrt(dx * dx + dy * dy)

            val toPointX = x - start.x
            val toPointY = y - start.y
            val projection = (toPointX * dx + toPointY * dy) / (length * length)
            val clampedProjection = projection.coerceIn(0f, 1f)

            val closestX = start.x + dx * clampedProjection
            val closestY = start.y + dy * clampedProjection

            val distance = kotlin.math.sqrt(
                (x - closestX) * (x - closestX) + (y - closestY) * (y - closestY)
            )

            if (distance <= pathWidth / 2) {
                return true
            }
        }
        return false
    }

    private fun placeTower(position: PointF, type: TowerType): Boolean {
        if (gold < type.cost) return false
        if (isPointOnPath(position.x, position.y)) return false

        // Check if too close to existing towers
        for (tower in towers) {
            val dx = position.x - tower.position.x
            val dy = position.y - tower.position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < 60f) return false
        }

        gold -= type.cost
        towers.add(Tower(position, type))
        listener?.onGoldChanged(gold)
        invalidate()
        return true
    }

    private fun updateGame() {
        val now = SystemClock.elapsedRealtime()
        val deltaTime = if (lastFrameTime > 0) {
            ((now - lastFrameTime) / 1000f).coerceAtMost(0.1f)
        } else {
            0.016f
        }
        lastFrameTime = now

        // Spawn enemies
        if (waveManager.shouldSpawnEnemy(now)) {
            val enemyType = waveManager.spawnEnemy(now)
            enemyType?.let {
                val startPoint = path.getStartPoint()
                if (startPoint != null && viewWidth > 0 && viewHeight > 0) {
                    enemies.add(Enemy(it, PointF(startPoint.x, startPoint.y), 0f))
                }
            }
        }

        // Update enemies
        val pathLength = path.getPathLength()
        val enemyIterator = enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            if (enemy.isDead) {
                gold += enemy.getReward()
                listener?.onGoldChanged(gold)
                enemyIterator.remove()
                continue
            }
            if (enemy.isReachedEnd) {
                lives--
                listener?.onLivesChanged(lives)
                enemyIterator.remove()
                if (lives <= 0) {
                    listener?.onGameOver(false)
                    isRunning = false
                    return
                }
                continue
            }

            enemy.move(enemy.type.speed * deltaTime * gameSpeed, pathLength)
            val newPosition = path.getPointAtDistance(enemy.distanceTraveled)
            if (newPosition != null) {
                enemy.position = newPosition
            }
        }

        // Update towers
        for (tower in towers) {
            tower.targetEnemy = tower.findTarget(enemies)
            if (tower.targetEnemy != null && tower.canShoot(now)) {
                val bullet = tower.shoot(now)
                if (bullet != null) {
                    bullets.add(bullet)
                }
            }
        }

        // Update bullets
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            if (!bullet.isActive) {
                bulletIterator.remove()
                continue
            }

            bullet.update(deltaTime * gameSpeed)

            // Check collision with enemies
            for (enemy in enemies) {
                if (bullet.checkHit(enemy, 20f)) {
                    break
                }
            }

            // Remove bullets that are off screen
            if (bullet.position.x < -50 || bullet.position.x > viewWidth + 50 ||
                bullet.position.y < -50 || bullet.position.y > viewHeight + 50
            ) {
                bullet.isActive = false
            }
        }

        // Check wave completion
        if (waveManager.isWaveComplete() && enemies.isEmpty()) {
            listener?.onWaveComplete()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
    }
}

