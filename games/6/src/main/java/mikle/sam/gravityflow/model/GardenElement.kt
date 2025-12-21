package mikle.sam.gravityflow.model

import android.graphics.PointF

enum class ElementType {
    STONE,      // Камень
    FLOWER,     // Цветок
    LEAF,       // Лист
    WATER       // Вода
}

class GardenElement(
    val type: ElementType,
    var position: PointF,
    val size: Float = 60f
) {
    var isPlaced = false
    var rotation = 0f
    var scale = 1f
    
    fun getColor(): Int {
        return when (type) {
            ElementType.STONE -> 0xFF8B7355.toInt()      // Коричневый
            ElementType.FLOWER -> 0xFFFF6B9D.toInt()     // Розовый
            ElementType.LEAF -> 0xFF4ECDC4.toInt()      // Зеленый
            ElementType.WATER -> 0xFF4A90E2.toInt()     // Синий
        }
    }
}


