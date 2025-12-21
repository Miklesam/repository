package mikle.sam.towerdefense.model

import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Tower(
    var position: PointF,
    val type: TowerType = TowerType.BASIC
) {
    var lastShotTime: Long = 0L
    var targetEnemy: Enemy? = null
    var level: Int = 1
    private val maxLevel = 5
    
    fun getDamage(): Int {
        return (type.damage * (1 + (level - 1) * 0.5f)).toInt()
    }
    
    fun getRange(): Float {
        return type.range * (1 + (level - 1) * 0.2f)
    }
    
    fun getFireRate(): Long {
        return (type.fireRate * (1 - (level - 1) * 0.1f)).toLong().coerceAtLeast(200L)
    }
    
    fun getUpgradeCost(): Int {
        return type.cost / 2 + (level - 1) * 25
    }
    
    fun canUpgrade(): Boolean {
        return level < maxLevel
    }
    
    fun upgrade(): Boolean {
        if (!canUpgrade()) return false
        level++
        return true
    }
    
    fun canShoot(currentTime: Long): Boolean {
        return currentTime - lastShotTime >= getFireRate()
    }
    
    fun findTarget(enemies: List<Enemy>): Enemy? {
        var closestEnemy: Enemy? = null
        var closestDistance = getRange()
        
        for (enemy in enemies) {
            if (enemy.isDead || enemy.isReachedEnd) continue
            
            val dx = enemy.position.x - position.x
            val dy = enemy.position.y - position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            
            if (distance <= getRange() && distance < closestDistance) {
                closestDistance = distance
                closestEnemy = enemy
            }
        }
        
        return closestEnemy
    }
    
    fun getAngleToTarget(target: PointF): Float {
        val dx = target.x - position.x
        val dy = target.y - position.y
        return atan2(dy, dx)
    }
    
    fun shoot(currentTime: Long): Bullet? {
        if (!canShoot(currentTime)) return null
        if (targetEnemy == null) return null
        
        lastShotTime = currentTime
        val angle = getAngleToTarget(targetEnemy!!.position)
        
        return Bullet(
            position = PointF(position.x, position.y),
            angle = angle,
            speed = type.bulletSpeed,
            damage = getDamage()
        )
    }
}

enum class TowerType(
    val cost: Int,
    val damage: Int,
    val range: Float,
    val fireRate: Long,
    val bulletSpeed: Float
) {
    BASIC(50, 20, 150f, 1000L, 300f),
    RAPID(75, 15, 120f, 500L, 350f),
    SNIPER(100, 50, 200f, 2000L, 400f)
}

