package mikle.sam.game2048

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class Tile(
    var value: Int,
    var x: Int,
    var y: Int
) {
    var targetX: Int = x
    var targetY: Int = y
    var currentX: Float = x.toFloat()
    var currentY: Float = y.toFloat()
    var mergedFrom: List<Tile>? = null
    var isAnimating = false
    var animationProgress = 0f
    var scale = 1f
    var isNew = false
    var isMerged = false
    
    fun savePosition() {
        targetX = x
        targetY = y
    }
    
    fun updatePosition() {
        x = targetX
        y = targetY
        currentX = x.toFloat()
        currentY = y.toFloat()
    }
    
    fun updateAnimation(deltaTime: Float): Boolean {
        if (!isAnimating) return false
        
        animationProgress += deltaTime * 4f // Animation speed
        if (animationProgress > 1f) {
            animationProgress = 1f
            isAnimating = false
            isNew = false
            isMerged = false
            scale = 1f
        }
        
        // Scale animation for new/merged tiles
        if (isNew) {
            scale = easeOut(animationProgress) // Scale from 0 to 1
        } else if (isMerged) {
            scale = if (animationProgress < 0.3f) {
                1f + animationProgress * 0.5f // Scale up
            } else {
                1.5f - (animationProgress - 0.3f) * 0.71f // Scale back down
            }
        }
        
        return true
    }
    
    private fun easeOut(t: Float): Float {
        return 1f - (1f - t) * (1f - t)
    }
    
    fun startMoveAnimation(newX: Int, newY: Int) {
        // For now, just update position immediately
        // We can add movement animations later if needed
        x = newX
        y = newY
        currentX = newX.toFloat()
        currentY = newY.toFloat()
        targetX = newX
        targetY = newY
    }
    
    fun startNewTileAnimation() {
        isNew = true
        isAnimating = true
        animationProgress = 0f
        scale = 0f
        currentX = x.toFloat()
        currentY = y.toFloat()
        targetX = x
        targetY = y
    }
    
    fun startMergeAnimation() {
        isMerged = true
        isAnimating = true
        animationProgress = 0f
        scale = 1f
        currentX = x.toFloat()
        currentY = y.toFloat()
        targetX = x
        targetY = y
    }
    
    fun getBackgroundColor(): Int {
        return when (value) {
            2 -> 0xFFEEE4DA.toInt()
            4 -> 0xFFEDE0C8.toInt()
            8 -> 0xFFF2B179.toInt()
            16 -> 0xFFF59563.toInt()
            32 -> 0xFFF67C5F.toInt()
            64 -> 0xFFF65E3B.toInt()
            128 -> 0xFFEDCF72.toInt()
            256 -> 0xFFEDCC61.toInt()
            512 -> 0xFFEDC850.toInt()
            1024 -> 0xFFEDC53F.toInt()
            2048 -> 0xFFEDC22E.toInt()
            else -> 0xFF3C3A32.toInt()
        }
    }
    
    fun getTextColor(): Int {
        return if (value <= 4) 0xFF776E65.toInt() else 0xFFF9F6F2.toInt()
    }
    
    fun draw(canvas: Canvas, tileSize: Float, margin: Float, gridStartX: Float, gridStartY: Float) {
        // Use animated positions
        val animatedX = gridStartX + currentX * (tileSize + margin) + margin
        val animatedY = gridStartY + currentY * (tileSize + margin) + margin
        val animatedSize = tileSize * scale - margin
        
        val left = animatedX
        val top = animatedY
        val right = left + animatedSize
        val bottom = top + animatedSize
        
        val rect = RectF(left, top, right, bottom)
        
        android.util.Log.d("Game2048", "Drawing tile value=$value at ($left, $top, $right, $bottom)")
        
        // Draw background
        val backgroundPaint = Paint().apply {
            color = getBackgroundColor()
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, backgroundPaint)
        
        // Draw text
        if (value != 0) {
            val textPaint = Paint().apply {
                color = getTextColor()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = if (value < 100) 48f else if (value < 1000) 40f else 32f
                isFakeBoldText = true
            }
            
            val text = value.toString()
            val textY = rect.centerY() + textPaint.textSize / 3
            canvas.drawText(text, rect.centerX(), textY, textPaint)
        }
    }
}
