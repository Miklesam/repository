package mikle.sam.towerdefense.model

import android.graphics.PointF

class Enemy(
    val type: EnemyType,
    var position: PointF = PointF(0f, 0f),
    var distanceTraveled: Float = 0f
) {
    var health: Int = type.maxHealth
    var isDead: Boolean = false
    var isReachedEnd: Boolean = false
    
    fun takeDamage(damage: Int) {
        health -= damage
        if (health <= 0) {
            isDead = true
        }
    }
    
    fun move(distance: Float, pathLength: Float) {
        distanceTraveled += distance
        if (distanceTraveled >= pathLength) {
            isReachedEnd = true
        }
    }
    
    fun getReward(): Int = type.reward
}

enum class EnemyType(val maxHealth: Int, val speed: Float, val reward: Int, val color: Int) {
    BASIC(80, 40f, 15, 0xFFE74C3C.toInt()),
    FAST(50, 60f, 20, 0xFFC0392B.toInt()),
    TANK(250, 25f, 40, 0xFF8E44AD.toInt()),
    ELITE(120, 50f, 30, 0xFFE67E22.toInt())
}

