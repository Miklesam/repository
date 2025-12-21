package mikle.sam.towerdefense.model

import android.graphics.PointF

class Path {
    private val waypoints = mutableListOf<PointF>()
    
    init {
        // Create a simple S-shaped path
        // This will be adjusted based on screen size
    }
    
    fun setWaypoints(points: List<PointF>) {
        waypoints.clear()
        waypoints.addAll(points)
    }
    
    fun getWaypoints(): List<PointF> = waypoints
    
    fun getStartPoint(): PointF? = waypoints.firstOrNull()
    
    fun getEndPoint(): PointF? = waypoints.lastOrNull()
    
    fun getPathLength(): Float {
        if (waypoints.size < 2) return 0f
        var length = 0f
        for (i in 0 until waypoints.size - 1) {
            val dx = waypoints[i + 1].x - waypoints[i].x
            val dy = waypoints[i + 1].y - waypoints[i].y
            length += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return length
    }
    
    fun getPointAtDistance(distance: Float): PointF? {
        if (waypoints.isEmpty()) return null
        if (distance <= 0) return PointF(waypoints[0].x, waypoints[0].y)
        
        var currentDistance = 0f
        for (i in 0 until waypoints.size - 1) {
            val dx = waypoints[i + 1].x - waypoints[i].x
            val dy = waypoints[i + 1].y - waypoints[i].y
            val segmentLength = kotlin.math.sqrt(dx * dx + dy * dy)
            
            if (currentDistance + segmentLength >= distance) {
                val t = (distance - currentDistance) / segmentLength
                return PointF(
                    waypoints[i].x + dx * t,
                    waypoints[i].y + dy * t
                )
            }
            currentDistance += segmentLength
        }
        
        return PointF(waypoints.last().x, waypoints.last().y)
    }
}


