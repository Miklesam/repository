package mikle.sam.gravityflow.model

import android.graphics.PointF

class Garden(val width: Float, val height: Float) {
    val elements = mutableListOf<GardenElement>()
    val patterns = mutableListOf<Pattern>()
    
    fun addElement(element: GardenElement): Boolean {
        // Проверяем, можно ли разместить элемент
        if (canPlaceElementPrivate(element)) {
            element.isPlaced = true
            elements.add(element)
            checkPatterns()
            return true
        }
        return false
    }
    
    fun removeElement(element: GardenElement) {
        elements.remove(element)
        checkPatterns()
    }
    
    fun canPlaceElement(element: GardenElement): Boolean {
        // Проверяем границы
        if (element.position.x < element.size || 
            element.position.x > width - element.size ||
            element.position.y < element.size || 
            element.position.y > height - element.size) {
            return false
        }
        
        // Проверяем пересечения с другими элементами
        for (existing in elements) {
            val dx = element.position.x - existing.position.x
            val dy = element.position.y - existing.position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance < (element.size + existing.size) * 0.8f) {
                return false
            }
        }
        
        return true
    }
    
    private fun canPlaceElementPrivate(element: GardenElement): Boolean {
        return canPlaceElement(element)
    }
    
    private fun checkPatterns() {
        patterns.clear()
        
        // Ищем паттерны из 3+ элементов одного типа рядом
        val typeGroups = elements.groupBy { it.type }
        
        for ((type, elementsOfType) in typeGroups) {
            if (elementsOfType.size >= 3) {
                // Проверяем, образуют ли они паттерн
                val pattern = findPattern(elementsOfType)
                if (pattern != null) {
                    patterns.add(pattern)
                }
            }
        }
    }
    
    private fun findPattern(elements: List<GardenElement>): Pattern? {
        // Простой паттерн: 3+ элемента одного типа в радиусе
        if (elements.size >= 3) {
            val center = PointF(
                elements.map { it.position.x }.average().toFloat(),
                elements.map { it.position.y }.average().toFloat()
            )
            val maxDistance = 200f
            
            val patternElements = elements.filter {
                val dx = it.position.x - center.x
                val dy = it.position.y - center.y
                kotlin.math.sqrt(dx * dx + dy * dy) <= maxDistance
            }
            
            if (patternElements.size >= 3) {
                return Pattern(patternElements, center)
            }
        }
        return null
    }
    
    fun clear() {
        elements.clear()
        patterns.clear()
    }
}

data class Pattern(
    val elements: List<GardenElement>,
    val center: PointF
) {
    fun getScore(): Int = elements.size * 10
}

