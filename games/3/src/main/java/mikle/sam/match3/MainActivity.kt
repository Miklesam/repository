package mikle.sam.match3

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    
    private lateinit var gameGrid: GridLayout
    private lateinit var scoreText: TextView
    private lateinit var movesText: TextView
    private lateinit var newGameButton: Button
    private lateinit var gameOverText: TextView
    
    private val boardSize = 8
    private val gemTypes = 6
    private var gameBoard = Array(boardSize) { IntArray(boardSize) }
    private var score = 0
    private var movesLeft = 30
    private var selectedGem: Pair<Int, Int>? = null
    private var isAnimating = false
    
    private val gemDrawables = arrayOf(
        R.drawable.gem_red,
        R.drawable.gem_blue,
        R.drawable.gem_green,
        R.drawable.gem_yellow,
        R.drawable.gem_purple,
        R.drawable.gem_orange
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        initializeGame()
    }
    
    private fun initViews() {
        gameGrid = findViewById(R.id.gameGrid)
        scoreText = findViewById(R.id.scoreText)
        movesText = findViewById(R.id.movesText)
        newGameButton = findViewById(R.id.newGameButton)
        gameOverText = findViewById(R.id.gameOverText)
    }
    
    private fun setupClickListeners() {
        newGameButton.setOnClickListener {
            startNewGame()
        }
    }
    
    private fun initializeGame() {
        generateInitialBoard()
        createGameGrid()
        updateUI()
    }
    
    private fun generateInitialBoard() {
        do {
            for (i in 0 until boardSize) {
                for (j in 0 until boardSize) {
                    gameBoard[i][j] = Random.nextInt(gemTypes)
                }
            }
        } while (hasMatches())
    }
    
    private fun createGameGrid() {
        gameGrid.removeAllViews()
        
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                val gemButton = Button(this)
                val layoutParams = GridLayout.LayoutParams()
                layoutParams.width = 0
                layoutParams.height = 0
                layoutParams.columnSpec = GridLayout.spec(j, 1f)
                layoutParams.rowSpec = GridLayout.spec(i, 1f)
                layoutParams.setMargins(2, 2, 2, 2)
                
                gemButton.layoutParams = layoutParams
                gemButton.setBackgroundResource(gemDrawables[gameBoard[i][j]])
                gemButton.tag = Pair(i, j)
                gemButton.setOnClickListener { onGemClick(it) }
                
                gameGrid.addView(gemButton)
            }
        }
    }
    
    private fun onGemClick(view: View) {
        if (isAnimating) return
        
        val position = view.tag as Pair<Int, Int>
        val row = position.first
        val col = position.second
        
        if (selectedGem == null) {
            // First gem selection
            selectedGem = position
            view.alpha = 0.7f
        } else {
            val (selectedRow, selectedCol) = selectedGem!!
            
            if (selectedRow == row && selectedCol == col) {
                // Deselect same gem
                view.alpha = 1.0f
                selectedGem = null
            } else if (areAdjacent(selectedRow, selectedCol, row, col)) {
                // Try to swap gems
                swapGems(selectedRow, selectedCol, row, col)
                selectedGem = null
            } else {
                // Select new gem
                val previousView = gameGrid.getChildAt(selectedRow * boardSize + selectedCol)
                previousView.alpha = 1.0f
                selectedGem = position
                view.alpha = 0.7f
            }
        }
    }
    
    private fun areAdjacent(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        val rowDiff = kotlin.math.abs(row1 - row2)
        val colDiff = kotlin.math.abs(col1 - col2)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }
    
    private fun swapGems(row1: Int, col1: Int, row2: Int, col2: Int) {
        // Swap gems in the board
        val temp = gameBoard[row1][col1]
        gameBoard[row1][col1] = gameBoard[row2][col2]
        gameBoard[row2][col2] = temp
        
        // Animate the swap
        animateSwap(row1, col1, row2, col2)
        
        // Check for matches after swap
        Handler(Looper.getMainLooper()).postDelayed({
            if (hasMatches()) {
                processMatches()
                movesLeft--
                updateUI()
                if (movesLeft <= 0) {
                    gameOver()
                }
            } else {
                // No matches, swap back
                swapGems(row1, col1, row2, col2)
            }
        }, 300)
    }
    
    private fun animateSwap(row1: Int, col1: Int, row2: Int, col2: Int) {
        val view1 = gameGrid.getChildAt(row1 * boardSize + col1)
        val view2 = gameGrid.getChildAt(row2 * boardSize + col2)
        
        val animator1X = ObjectAnimator.ofFloat(view1, "translationX", 0f, view2.x - view1.x)
        val animator1Y = ObjectAnimator.ofFloat(view1, "translationY", 0f, view2.y - view1.y)
        val animator2X = ObjectAnimator.ofFloat(view2, "translationX", 0f, view1.x - view2.x)
        val animator2Y = ObjectAnimator.ofFloat(view2, "translationY", 0f, view1.y - view2.y)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator1X, animator1Y, animator2X, animator2Y)
        animatorSet.duration = 150
        animatorSet.start()
        
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Update button backgrounds
                view1.setBackgroundResource(gemDrawables[gameBoard[row1][col1]])
                view2.setBackgroundResource(gemDrawables[gameBoard[row2][col2]])
                
                // Reset positions
                view1.translationX = 0f
                view1.translationY = 0f
                view2.translationX = 0f
                view2.translationY = 0f
                
                view1.alpha = 1.0f
                view2.alpha = 1.0f
            }
        })
    }
    
    private fun hasMatches(): Boolean {
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                if (checkMatch(i, j)) {
                    return true
                }
            }
        }
        return false
    }
    
    private fun checkMatch(row: Int, col: Int): Boolean {
        val gemType = gameBoard[row][col]
        
        // Check horizontal matches
        var count = 1
        var left = col - 1
        while (left >= 0 && gameBoard[row][left] == gemType) {
            count++
            left--
        }
        var right = col + 1
        while (right < boardSize && gameBoard[row][right] == gemType) {
            count++
            right++
        }
        if (count >= 3) return true
        
        // Check vertical matches
        count = 1
        var up = row - 1
        while (up >= 0 && gameBoard[up][col] == gemType) {
            count++
            up--
        }
        var down = row + 1
        while (down < boardSize && gameBoard[down][col] == gemType) {
            count++
            down++
        }
        return count >= 3
    }
    
    private fun processMatches() {
        val matchedGems = mutableSetOf<Pair<Int, Int>>()
        
        // Find all matched gems
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                if (checkMatch(i, j)) {
                    findMatchedGems(i, j, matchedGems)
                }
            }
        }
        
        // Remove matched gems and drop new ones
        if (matchedGems.isNotEmpty()) {
            score += matchedGems.size * 10
            animateMatchedGems(matchedGems) {
                removeMatchedGems(matchedGems)
                dropGemsWithAnimation()
                fillEmptySpacesWithAnimation()
                
                // Check for chain reactions
                Handler(Looper.getMainLooper()).postDelayed({
                    if (hasMatches()) {
                        processMatches()
                    }
                }, 800)
            }
        }
    }
    
    private fun findMatchedGems(row: Int, col: Int, matchedGems: MutableSet<Pair<Int, Int>>) {
        val gemType = gameBoard[row][col]
        
        // Find horizontal matches
        val horizontalMatches = mutableListOf<Pair<Int, Int>>()
        var left = col
        while (left >= 0 && gameBoard[row][left] == gemType) {
            horizontalMatches.add(Pair(row, left))
            left--
        }
        var right = col + 1
        while (right < boardSize && gameBoard[row][right] == gemType) {
            horizontalMatches.add(Pair(row, right))
            right++
        }
        if (horizontalMatches.size >= 3) {
            matchedGems.addAll(horizontalMatches)
        }
        
        // Find vertical matches
        val verticalMatches = mutableListOf<Pair<Int, Int>>()
        var up = row
        while (up >= 0 && gameBoard[up][col] == gemType) {
            verticalMatches.add(Pair(up, col))
            up--
        }
        var down = row + 1
        while (down < boardSize && gameBoard[down][col] == gemType) {
            verticalMatches.add(Pair(down, col))
            down++
        }
        if (verticalMatches.size >= 3) {
            matchedGems.addAll(verticalMatches)
        }
    }
    
    private fun animateMatchedGems(matchedGems: Set<Pair<Int, Int>>, onComplete: () -> Unit) {
        isAnimating = true
        val animators = mutableListOf<ObjectAnimator>()
        
        for ((row, col) in matchedGems) {
            val buttonIndex = row * boardSize + col
            val button = gameGrid.getChildAt(buttonIndex)
            
            // First, scale up slightly to highlight the match
            val scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.2f)
            val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.2f)
            scaleUpX.duration = 150
            scaleUpY.duration = 150
            
            // Then scale down and fade out
            val scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1.2f, 0f)
            val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1.2f, 0f)
            val alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f)
            
            scaleDownX.duration = 200
            scaleDownY.duration = 200
            alpha.duration = 200
            alpha.startDelay = 100
            
            // Add rotation for extra effect
            val rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f)
            rotation.duration = 300
            
            animators.add(scaleUpX)
            animators.add(scaleUpY)
            animators.add(scaleDownX)
            animators.add(scaleDownY)
            animators.add(alpha)
            animators.add(rotation)
        }
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animators as Collection<android.animation.Animator>)
        animatorSet.start()
        
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onComplete()
                isAnimating = false
            }
        })
    }
    
    private fun removeMatchedGems(matchedGems: Set<Pair<Int, Int>>) {
        for ((row, col) in matchedGems) {
            gameBoard[row][col] = -1 // Mark as empty
        }
    }
    
    private fun dropGemsWithAnimation() {
        isAnimating = true
        val animators = mutableListOf<ObjectAnimator>()
        
        // Calculate new positions for gems
        val newPositions = Array(boardSize) { IntArray(boardSize) }
        for (col in 0 until boardSize) {
            var writeIndex = boardSize - 1
            for (row in boardSize - 1 downTo 0) {
                if (gameBoard[row][col] != -1) {
                    newPositions[writeIndex][col] = gameBoard[row][col]
                    writeIndex--
                }
            }
        }
        
        // Animate gems falling down
        for (col in 0 until boardSize) {
            for (row in 0 until boardSize) {
                if (gameBoard[row][col] != -1) {
                    val buttonIndex = row * boardSize + col
                    val button = gameGrid.getChildAt(buttonIndex)
                    
                    // Find where this gem should fall to
                    var newRow = row
                    while (newRow < boardSize - 1 && newPositions[newRow + 1][col] == 0) {
                        newRow++
                    }
                    
                    if (newRow != row) {
                        val targetButton = gameGrid.getChildAt(newRow * boardSize + col)
                        val translateY = ObjectAnimator.ofFloat(button, "translationY", 0f, targetButton.y - button.y)
                        translateY.duration = 400
                        translateY.startDelay = (row * 50).toLong() // Stagger the animations
                        animators.add(translateY)
                    }
                }
            }
        }
        
        // Update the game board after animations start
        Handler(Looper.getMainLooper()).postDelayed({
            for (col in 0 until boardSize) {
                var writeIndex = boardSize - 1
                for (row in boardSize - 1 downTo 0) {
                    if (gameBoard[row][col] != -1) {
                        if (writeIndex != row) {
                            gameBoard[writeIndex][col] = gameBoard[row][col]
                            gameBoard[row][col] = -1
                        }
                        writeIndex--
                    }
                }
            }
            updateGameGrid()
        }, 100)
        
        // Reset positions after animations complete
        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0 until gameGrid.childCount) {
                val button = gameGrid.getChildAt(i)
                button.translationY = 0f
            }
            isAnimating = false
        }, 600)
    }
    
    private fun fillEmptySpacesWithAnimation() {
        isAnimating = true
        val animators = mutableListOf<ObjectAnimator>()
        
        // Fill empty spaces with new gems
        for (col in 0 until boardSize) {
            for (row in 0 until boardSize) {
                if (gameBoard[row][col] == -1) {
                    gameBoard[row][col] = Random.nextInt(gemTypes)
                    val buttonIndex = row * boardSize + col
                    val button = gameGrid.getChildAt(buttonIndex)
                    
                    // Start from above the screen
                    button.translationY = -button.height.toFloat()
                    button.alpha = 0f
                    button.scaleX = 0.5f
                    button.scaleY = 0.5f
                    
                    // Animate falling down and appearing
                    val translateY = ObjectAnimator.ofFloat(button, "translationY", -button.height.toFloat(), 0f)
                    val alpha = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f)
                    val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 0.5f, 1f)
                    val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 0.5f, 1f)
                    
                    translateY.duration = 500
                    alpha.duration = 300
                    scaleX.duration = 300
                    scaleY.duration = 300
                    
                    translateY.startDelay = (row * 100).toLong() // Stagger the animations
                    
                    animators.add(translateY)
                    animators.add(alpha)
                    animators.add(scaleX)
                    animators.add(scaleY)
                }
            }
        }
        
        // Update button backgrounds
        updateGameGrid()
        
        // Reset positions after animations complete
        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0 until gameGrid.childCount) {
                val button = gameGrid.getChildAt(i)
                button.translationY = 0f
                button.alpha = 1f
                button.scaleX = 1f
                button.scaleY = 1f
            }
            isAnimating = false
        }, 800)
    }
    
    private fun dropGems() {
        for (col in 0 until boardSize) {
            var writeIndex = boardSize - 1
            for (row in boardSize - 1 downTo 0) {
                if (gameBoard[row][col] != -1) {
                    if (writeIndex != row) {
                        gameBoard[writeIndex][col] = gameBoard[row][col]
                        gameBoard[row][col] = -1
                    }
                    writeIndex--
                }
            }
        }
    }
    
    private fun fillEmptySpaces() {
        for (col in 0 until boardSize) {
            for (row in 0 until boardSize) {
                if (gameBoard[row][col] == -1) {
                    gameBoard[row][col] = Random.nextInt(gemTypes)
                }
            }
        }
    }
    
    private fun updateGameGrid() {
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                val buttonIndex = i * boardSize + j
                val button = gameGrid.getChildAt(buttonIndex) as Button
                button.setBackgroundResource(gemDrawables[gameBoard[i][j]])
            }
        }
    }
    
    private fun updateUI() {
        scoreText.text = getString(R.string.score, score)
        movesText.text = getString(R.string.moves, movesLeft)
    }
    
    private fun startNewGame() {
        score = 0
        movesLeft = 30
        selectedGem = null
        gameOverText.visibility = View.GONE
        initializeGame()
    }
    
    private fun gameOver() {
        gameOverText.text = getString(R.string.final_score, score)
        gameOverText.visibility = View.VISIBLE
    }
}
