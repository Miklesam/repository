package mikle.sam.towerdefense.model

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

class Bullet(
    var position: PointF,
    val angle: Float,
    val speed: Float,
    val damage: Int
) {
    var isActive: Boolean = true
    
    fun update(deltaTime: Float) {
        if (!isActive) return
        
        val dx = cos(angle) * speed * deltaTime
        val dy = sin(angle) * speed * deltaTime
        position.x += dx
        position.y += dy
    }
    
    fun checkHit(enemy: Enemy, hitRadius: Float): Boolean {
        if (!isActive || enemy.isDead) return false
        
        val dx = position.x - enemy.position.x
        val dy = position.y - enemy.position.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        
        if (distance <= hitRadius) {
            enemy.takeDamage(damage)
            isActive = false
            return true
        }
        
        return false
    }
}


