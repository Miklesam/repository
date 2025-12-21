package mikle.sam.gravityflow.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import mikle.sam.gravityflow.R
import mikle.sam.gravityflow.model.*

class ZenGardenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface GameListener {
        fun onScoreChanged(score: Int)
        fun onPatternCreated(patternType: String, score: Int)
    }

    private var listener: GameListener? = null
    
    // Game objects
    private var garden: Garden? = null
    private var selectedElementType: ElementType = ElementType.STONE
    private var previewElement: GardenElement? = null
    private var draggedElement: GardenElement? = null
    private var dragOffset = PointF(0f, 0f)
    
    // Game state
    private var score = 0
    private var combo = 0
    private var lastPatternTime = 0L
    
    // Animation
    private val animations = mutableListOf<Animation>()
    private var lastFrameTime = 0L
    
    // Drawing
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.garden_background)
        style = Paint.Style.FILL
    }
    
    private val elementPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 150
    }
    
    private val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pattern_glow)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 200
    }
    
    private val frameRunnable = object : Runnable {
        override fun run() {
            updateAnimations()
            invalidate()
            postDelayed(this, 16) // ~60 FPS
        }
    }

    fun setGameListener(listener: GameListener?) {
        this.listener = listener
    }

    fun setSelectedElementType(type: ElementType) {
        selectedElementType = type
        previewElement = null
        invalidate()
    }

    fun resetGame() {
        score = 0
        combo = 0
        garden?.clear()
        animations.clear()
        listener?.onScoreChanged(score)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (garden == null) {
            garden = Garden(w.toFloat(), h.toFloat())
        } else {
            garden = Garden(w.toFloat(), h.toFloat())
        }
        removeCallbacks(frameRunnable)
        post(frameRunnable)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Проверяем, не нажали ли на существующий элемент для перемещения
                val element = findElementAt(x, y)
                if (element != null) {
                    draggedElement = element
                    dragOffset.x = x - element.position.x
                    dragOffset.y = y - element.position.y
                    return true
                }
                
                // Иначе создаем превью нового элемента
                previewElement = GardenElement(selectedElementType, PointF(x, y))
                invalidate()
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val dragged = draggedElement
                if (dragged != null) {
                    // Перемещаем существующий элемент
                    dragged.position.x = x - dragOffset.x
                    dragged.position.y = y - dragOffset.y
                    invalidate()
                    return true
                }
                val preview = previewElement
                if (preview != null) {
                    // Обновляем превью
                    preview.position.x = x
                    preview.position.y = y
                    invalidate()
                    return true
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                val garden = this.garden ?: return true
                
                val dragged = draggedElement
                if (dragged != null) {
                    // Завершаем перемещение
                    if (!garden.addElement(dragged)) {
                        // Если не удалось разместить, возвращаем на место
                        // (в реальной игре можно добавить анимацию возврата)
                    }
                    draggedElement = null
                    checkPatterns(garden)
                    invalidate()
                    return true
                }
                val preview = previewElement
                if (preview != null) {
                    // Размещаем новый элемент
                    if (garden.addElement(preview)) {
                        // Успешно размещен
                        createPlaceAnimation(preview)
                        checkPatterns(garden)
                    } else {
                        // Не удалось разместить
                        createFailAnimation(preview.position)
                    }
                    previewElement = null
                    invalidate()
                    return true
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findElementAt(x: Float, y: Float): GardenElement? {
        val garden = this.garden ?: return null
        for (element in garden.elements.reversed()) {
            val dx = x - element.position.x
            val dy = y - element.position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < element.size) {
                return element
            }
        }
        return null
    }

    private fun checkPatterns(garden: Garden) {
        for (pattern in garden.patterns) {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastPatternTime > 500) { // Защита от спама
                val patternScore = pattern.getScore() * (1 + combo / 10)
                score += patternScore
                combo++
                
                createPatternAnimation(pattern)
                listener?.onPatternCreated(pattern.elements[0].type.name, patternScore)
                listener?.onScoreChanged(score)
                
                lastPatternTime = currentTime
            }
        }
    }

    private fun createPlaceAnimation(element: GardenElement) {
        animations.add(ScaleAnimation(element, 0f, 1f, 300))
    }

    private fun createFailAnimation(position: PointF) {
        // Можно добавить анимацию неудачи
    }

    private fun createPatternAnimation(pattern: Pattern) {
        animations.add(PulseAnimation(pattern.center, 400))
        // Удаляем элементы паттерна через небольшую задержку
        postDelayed({
            pattern.elements.forEach { garden?.removeElement(it) }
            combo = 0 // Сбрасываем комбо если не создали новый паттерн
        }, 500)
    }

    private fun updateAnimations() {
        val currentTime = SystemClock.elapsedRealtime()
        val deltaTime = if (lastFrameTime > 0) {
            ((currentTime - lastFrameTime) / 1000f).coerceAtMost(0.1f)
        } else {
            0.016f
        }
        lastFrameTime = currentTime
        
        val iterator = animations.iterator()
        while (iterator.hasNext()) {
            val anim = iterator.next()
            if (anim.update(deltaTime)) {
                iterator.remove()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val garden = this.garden ?: return
        
        // Рисуем фон
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Рисуем паттерны (подсветка)
        for (pattern in garden.patterns) {
            val radius = 150f
            canvas.drawCircle(pattern.center.x, pattern.center.y, radius, patternPaint)
        }
        
        // Рисуем размещенные элементы
        for (element in garden.elements) {
            drawElement(canvas, element, elementPaint)
        }
        
        // Рисуем превью элемента
        if (previewElement != null) {
            val testElement = GardenElement(
                previewElement!!.type,
                previewElement!!.position,
                previewElement!!.size
            )
            val canPlace = garden.canPlaceElement(testElement)
            
            previewPaint.alpha = if (canPlace) 150 else 80
            drawElement(canvas, previewElement!!, previewPaint)
        }
        
        // Рисуем анимации
        for (anim in animations) {
            anim.draw(canvas)
        }
    }

    private fun drawElement(canvas: Canvas, element: GardenElement, paint: Paint) {
        paint.color = element.getColor()
        
        canvas.save()
        canvas.translate(element.position.x, element.position.y)
        canvas.rotate(element.rotation)
        canvas.scale(element.scale, element.scale)
        
        when (element.type) {
            ElementType.STONE -> {
                canvas.drawCircle(0f, 0f, element.size, paint)
            }
            ElementType.FLOWER -> {
                // Рисуем цветок (круг с лепестками)
                canvas.drawCircle(0f, 0f, element.size * 0.6f, paint)
                for (i in 0 until 5) {
                    val angle = i * 72f * Math.PI / 180
                    val x = kotlin.math.cos(angle).toFloat() * element.size * 0.4f
                    val y = kotlin.math.sin(angle).toFloat() * element.size * 0.4f
                    canvas.drawCircle(x, y, element.size * 0.3f, paint)
                }
            }
            ElementType.LEAF -> {
                // Рисуем лист (овал)
                canvas.drawOval(
                    -element.size * 0.7f, -element.size * 0.4f,
                    element.size * 0.7f, element.size * 0.4f,
                    paint
                )
            }
            ElementType.WATER -> {
                // Рисуем воду (волнистый круг)
                val gradient = RadialGradient(
                    0f, 0f, element.size,
                    element.getColor(), element.getColor() and 0x80FFFFFF.toInt(),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawCircle(0f, 0f, element.size, paint)
                paint.shader = null
            }
        }
        
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frameRunnable)
    }

    fun getScore(): Int = score
    fun getCombo(): Int = combo
}

// Простые анимации
abstract class Animation {
    abstract fun update(deltaTime: Float): Boolean // возвращает true если анимация завершена
    abstract fun draw(canvas: Canvas)
}

class ScaleAnimation(
    private val element: GardenElement,
    private val startScale: Float,
    private val endScale: Float,
    private val duration: Long
) : Animation() {
    private val startTime = SystemClock.elapsedRealtime()
    
    override fun update(deltaTime: Float): Boolean {
        val elapsed = SystemClock.elapsedRealtime() - startTime
        if (elapsed >= duration) {
            element.scale = endScale
            return true
        }
        val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        element.scale = startScale + (endScale - startScale) * progress
        return false
    }
    
    override fun draw(canvas: Canvas) {}
}

class PulseAnimation(
    private val center: PointF,
    private val duration: Long
) : Animation() {
    private val startTime = SystemClock.elapsedRealtime()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    
    override fun update(deltaTime: Float): Boolean {
        return SystemClock.elapsedRealtime() - startTime >= duration
    }
    
    override fun draw(canvas: Canvas) {
        val elapsed = SystemClock.elapsedRealtime() - startTime
        val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        val radius = 100f + progress * 100f
        paint.alpha = ((1f - progress) * 100).toInt()
        canvas.drawCircle(center.x, center.y, radius, paint)
    }
}

