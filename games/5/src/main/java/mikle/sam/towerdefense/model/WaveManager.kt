package mikle.sam.towerdefense.model

class WaveManager {
    private var currentWave = 0
    private var enemiesInWave = 0
    private var enemiesSpawned = 0
    private var lastSpawnTime = 0L
    private val spawnInterval = 1000L // 1 second between spawns
    
    fun startWave(currentTime: Long): WaveInfo {
        currentWave++
        enemiesInWave = 6 + currentWave * 3
        enemiesSpawned = 0
        lastSpawnTime = currentTime
        return WaveInfo(currentWave, enemiesInWave)
    }
    
    fun shouldSpawnEnemy(currentTime: Long): Boolean {
        if (enemiesSpawned >= enemiesInWave) return false
        return currentTime - lastSpawnTime >= spawnInterval
    }
    
    fun spawnEnemy(currentTime: Long): EnemyType? {
        if (!shouldSpawnEnemy(currentTime)) return null
        
        enemiesSpawned++
        lastSpawnTime = currentTime
        
        // Determine enemy type based on wave
        val rand = kotlin.random.Random.nextFloat()
        return when {
            currentWave >= 7 && rand < 0.15f -> EnemyType.TANK
            currentWave >= 5 && rand < 0.2f -> EnemyType.ELITE
            currentWave >= 3 && rand < 0.3f -> EnemyType.FAST
            else -> EnemyType.BASIC
        }
    }
    
    fun isWaveComplete(): Boolean {
        return enemiesSpawned >= enemiesInWave
    }
    
    fun getCurrentWave(): Int = currentWave
}

data class WaveInfo(val waveNumber: Int, val enemyCount: Int)

