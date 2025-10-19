package mikle.sam.game2048

import kotlin.random.Random

class Game2048(private val size: Int = 4) {
    private val grid = Array(size) { x -> Array(size) { y -> Tile(0, x, y) } }
    private var score = 0
    private var won = false
    private var over = false
    
    init {
        addRandomTile()
        addRandomTile()
    }
    
    fun getGrid(): Array<Array<Tile>> = grid
    fun getScore(): Int = score
    fun isWon(): Boolean = won
    fun isOver(): Boolean = over
    
    fun move(direction: Direction): Boolean {
        if (over) return false
        
        var moved = false
        
        when (direction) {
            Direction.UP -> moved = moveUp()
            Direction.DOWN -> moved = moveDown()
            Direction.LEFT -> moved = moveLeft()
            Direction.RIGHT -> moved = moveRight()
        }
        
        if (moved) {
            addRandomTile()
            if (!movesAvailable()) {
                over = true
            }
        }
        
        return moved
    }
    
    private fun moveLeft(): Boolean {
        var moved = false
        
        for (rowIndex in 0 until size) {
            // Save original row to check if anything changed
            val originalRow = IntArray(size) { grid[rowIndex][it].value }
            
            val values = mutableListOf<Int>()
            
            // Collect non-zero values from left to right
            for (colIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != 0) {
                    values.add(grid[rowIndex][colIndex].value)
                }
            }
            
            if (values.isEmpty()) continue
            
            // Merge adjacent equal values
            val mergedValues = mutableListOf<Int>()
            var i = 0
            while (i < values.size) {
                if (i < values.size - 1 && values[i] == values[i + 1]) {
                    // Merge
                    mergedValues.add(values[i] * 2)
                    score += values[i] * 2
                    if (values[i] * 2 == 2048) won = true
                    i += 2
                } else {
                    mergedValues.add(values[i])
                    i++
                }
            }
            
            // Update the row (fill from left edge)
            for (colIndex in 0 until size) {
                if (colIndex < mergedValues.size) {
                    grid[rowIndex][colIndex].value = mergedValues[colIndex]
                    grid[rowIndex][colIndex].x = rowIndex
                    grid[rowIndex][colIndex].y = colIndex
                } else {
                    grid[rowIndex][colIndex].value = 0
                    grid[rowIndex][colIndex].x = rowIndex
                    grid[rowIndex][colIndex].y = colIndex
                }
            }
            
            // Check if anything changed
            for (colIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != originalRow[colIndex]) {
                    moved = true
                    break
                }
            }
        }
        
        return moved
    }
    
    private fun moveRight(): Boolean {
        var moved = false
        
        for (rowIndex in 0 until size) {
            // Save original row to check if anything changed
            val originalRow = IntArray(size) { grid[rowIndex][it].value }
            
            val values = mutableListOf<Int>()
            
            // Collect non-zero values from left to right
            for (colIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != 0) {
                    values.add(grid[rowIndex][colIndex].value)
                }
            }
            
            if (values.isEmpty()) continue
            
            // Merge adjacent equal values (from right to left)
            val mergedValues = mutableListOf<Int>()
            var i = values.size - 1
            while (i >= 0) {
                if (i > 0 && values[i] == values[i - 1]) {
                    // Merge
                    mergedValues.add(0, values[i] * 2)
                    score += values[i] * 2
                    if (values[i] * 2 == 2048) won = true
                    i -= 2
                } else {
                    mergedValues.add(0, values[i])
                    i--
                }
            }
            
            // Update the row (fill from right edge)
            for (colIndex in 0 until size) {
                val fillIndex = size - 1 - colIndex
                if (colIndex < mergedValues.size) {
                    grid[rowIndex][fillIndex].value = mergedValues[mergedValues.size - 1 - colIndex]
                    grid[rowIndex][fillIndex].x = rowIndex
                    grid[rowIndex][fillIndex].y = fillIndex
                } else {
                    grid[rowIndex][fillIndex].value = 0
                    grid[rowIndex][fillIndex].x = rowIndex
                    grid[rowIndex][fillIndex].y = fillIndex
                }
            }
            
            // Check if anything changed
            for (colIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != originalRow[colIndex]) {
                    moved = true
                    break
                }
            }
        }
        
        return moved
    }
    
    private fun moveUp(): Boolean {
        var moved = false
        
        for (colIndex in 0 until size) {
            // Save original column to check if anything changed
            val originalColumn = IntArray(size) { grid[it][colIndex].value }
            
            val values = mutableListOf<Int>()
            
            // Collect non-zero values from top to bottom
            for (rowIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != 0) {
                    values.add(grid[rowIndex][colIndex].value)
                }
            }
            
            if (values.isEmpty()) continue
            
            // Merge adjacent equal values
            val mergedValues = mutableListOf<Int>()
            var i = 0
            while (i < values.size) {
                if (i < values.size - 1 && values[i] == values[i + 1]) {
                    // Merge
                    mergedValues.add(values[i] * 2)
                    score += values[i] * 2
                    if (values[i] * 2 == 2048) won = true
                    i += 2
                } else {
                    mergedValues.add(values[i])
                    i++
                }
            }
            
            // Update the column (fill from top edge)
            for (rowIndex in 0 until size) {
                if (rowIndex < mergedValues.size) {
                    grid[rowIndex][colIndex].value = mergedValues[rowIndex]
                    grid[rowIndex][colIndex].x = rowIndex
                    grid[rowIndex][colIndex].y = colIndex
                } else {
                    grid[rowIndex][colIndex].value = 0
                    grid[rowIndex][colIndex].x = rowIndex
                    grid[rowIndex][colIndex].y = colIndex
                }
            }
            
            // Check if anything changed
            for (rowIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != originalColumn[rowIndex]) {
                    moved = true
                    break
                }
            }
        }
        
        return moved
    }
    
    private fun moveDown(): Boolean {
        var moved = false
        
        for (colIndex in 0 until size) {
            // Save original column to check if anything changed
            val originalColumn = IntArray(size) { grid[it][colIndex].value }
            
            val values = mutableListOf<Int>()
            
            // Collect non-zero values from top to bottom
            for (rowIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != 0) {
                    values.add(grid[rowIndex][colIndex].value)
                }
            }
            
            if (values.isEmpty()) continue
            
            // Merge adjacent equal values (from bottom to top)
            val mergedValues = mutableListOf<Int>()
            var i = values.size - 1
            while (i >= 0) {
                if (i > 0 && values[i] == values[i - 1]) {
                    // Merge
                    mergedValues.add(0, values[i] * 2)
                    score += values[i] * 2
                    if (values[i] * 2 == 2048) won = true
                    i -= 2
                } else {
                    mergedValues.add(0, values[i])
                    i--
                }
            }
            
            // Update the column (fill from bottom edge)
            for (rowIndex in 0 until size) {
                val fillIndex = size - 1 - rowIndex
                if (rowIndex < mergedValues.size) {
                    grid[fillIndex][colIndex].value = mergedValues[mergedValues.size - 1 - rowIndex]
                    grid[fillIndex][colIndex].x = fillIndex
                    grid[fillIndex][colIndex].y = colIndex
                } else {
                    grid[fillIndex][colIndex].value = 0
                    grid[fillIndex][colIndex].x = fillIndex
                    grid[fillIndex][colIndex].y = colIndex
                }
            }
            
            // Check if anything changed
            for (rowIndex in 0 until size) {
                if (grid[rowIndex][colIndex].value != originalColumn[rowIndex]) {
                    moved = true
                    break
                }
            }
        }
        
        return moved
    }
    
    private fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (grid[x][y].value == 0) {
                    emptyCells.add(Pair(x, y))
                }
            }
        }
        
        if (emptyCells.isNotEmpty()) {
            val randomCell = emptyCells[Random.nextInt(emptyCells.size)]
            val value = if (Random.nextFloat() < 0.9f) 2 else 4
            grid[randomCell.first][randomCell.second].value = value
            grid[randomCell.first][randomCell.second].startNewTileAnimation()
        }
    }
    
    private fun movesAvailable(): Boolean {
        // Check for empty cells
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (grid[x][y].value == 0) return true
            }
        }
        
        // Check for possible merges
        for (x in 0 until size) {
            for (y in 0 until size) {
                val current = grid[x][y].value
                if (current != 0) {
                    // Check right neighbor
                    if (y < size - 1 && grid[x][y + 1].value == current) return true
                    // Check bottom neighbor
                    if (x < size - 1 && grid[x + 1][y].value == current) return true
                }
            }
        }
        
        return false
    }
    
    fun reset() {
        score = 0
        won = false
        over = false
        for (x in 0 until size) {
            for (y in 0 until size) {
                grid[x][y].value = 0
                grid[x][y].isAnimating = false
                grid[x][y].scale = 1f
                grid[x][y].currentX = x.toFloat()
                grid[x][y].currentY = y.toFloat()
            }
        }
        addRandomTile()
        addRandomTile()
    }
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}