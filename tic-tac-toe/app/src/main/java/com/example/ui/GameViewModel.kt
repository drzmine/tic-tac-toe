package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameMatch
import com.example.data.GameMatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface GameStatus {
    object Menu : GameStatus
    object Active : GameStatus
    data class Win(val winner: String, val cells: List<Int>) : GameStatus
    object Draw : GameStatus
}

data class GameSetup(
    val playerXName: String = "Player X",
    val playerOName: String = "AI Opponent",
    val isVsAi: Boolean = true,
    val aiDifficulty: String = "Medium" // "Easy", "Medium", "Hard"
)

class GameViewModel(
    application: Application,
    private val repository: GameMatchRepository
) : AndroidViewModel(application) {

    private val _board = MutableStateFlow<List<String?>>(List(9) { null })
    val board = _board.asStateFlow()

    private val _currentTurn = MutableStateFlow("X") // "X" or "O"
    val currentTurn = _currentTurn.asStateFlow()

    private val _gameStatus = MutableStateFlow<GameStatus>(GameStatus.Menu)
    val gameStatus = _gameStatus.asStateFlow()

    private val _gameSetup = MutableStateFlow(GameSetup())
    val gameSetup = _gameSetup.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking = _isAiThinking.asStateFlow()

    // Timer and stats tracking
    private var gameStartTime: Long = 0
    private var moveCount = 0

    // Match history
    val matchHistory: StateFlow<List<GameMatch>> = repository.allMatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startNewGame(setup: GameSetup) {
        _gameSetup.value = setup
        _board.value = List(9) { null }
        _currentTurn.value = "X"
        _gameStatus.value = GameStatus.Active
        gameStartTime = System.currentTimeMillis()
        moveCount = 0
        _isAiThinking.value = false
    }

    fun makeMove(index: Int) {
        val currentStatus = _gameStatus.value
        if (currentStatus != GameStatus.Active || _board.value[index] != null || _isAiThinking.value) {
            return
        }

        executeMove(index)
    }

    private fun executeMove(index: Int) {
        val currentSymbol = _currentTurn.value
        val newBoard = _board.value.toMutableList()
        newBoard[index] = currentSymbol
        _board.value = newBoard
        moveCount++

        val winningCells = checkWin(newBoard, currentSymbol)
        if (winningCells != null) {
            _gameStatus.value = GameStatus.Win(currentSymbol, winningCells)
            saveMatchToHistory(currentSymbol)
            return
        }

        if (newBoard.none { it == null }) {
            _gameStatus.value = GameStatus.Draw
            saveMatchToHistory("Draw")
            return
        }

        // Toggle turn
        val nextTurn = if (currentSymbol == "X") "O" else "X"
        _currentTurn.value = nextTurn

        // If VS AI and next turn is O (the AI), let AI move
        val setup = _gameSetup.value
        if (setup.isVsAi && nextTurn == "O") {
            triggerAiMove()
        }
    }

    private fun triggerAiMove() {
        viewModelScope.launch {
            _isAiThinking.value = true
            // Dynamic typing feedback style delay for realistic AI feel
            delay(Random.nextLong(400, 800))
            val aiMove = calculateAiMove(_board.value, _gameSetup.value.aiDifficulty)
            if (aiMove != -1) {
                executeMove(aiMove)
            }
            _isAiThinking.value = false
        }
    }

    private fun calculateAiMove(currentBoard: List<String?>, difficulty: String): Int {
        val availableIndices = currentBoard.indices.filter { currentBoard[it] == null }
        if (availableIndices.isEmpty()) return -1

        return when (difficulty) {
            "Easy" -> {
                availableIndices.random()
            }
            "Medium" -> {
                // 1. Can AI win in 1 move?
                for (index in availableIndices) {
                    val testBoard = currentBoard.toMutableList()
                    testBoard[index] = "O"
                    if (checkWin(testBoard, "O") != null) return index
                }
                // 2. Can player win in 1 move? Block them!
                for (index in availableIndices) {
                    val testBoard = currentBoard.toMutableList()
                    testBoard[index] = "X"
                    if (checkWin(testBoard, "X") != null) return index
                }
                // 3. Otherwise, 50% minimax / 50% random
                if (Random.nextFloat() < 0.5f) {
                    getBestMoveMinimax(currentBoard)
                } else {
                    availableIndices.random()
                }
            }
            "Hard" -> {
                // Perfect AI (Minimax)
                getBestMoveMinimax(currentBoard)
            }
            else -> availableIndices.random()
        }
    }

    private fun getBestMoveMinimax(currentBoard: List<String?>): Int {
        var bestScore = Int.MIN_VALUE
        var bestMove = -1

        for (i in currentBoard.indices) {
            if (currentBoard[i] == null) {
                val testBoard = currentBoard.toMutableList()
                testBoard[i] = "O"
                val score = minimax(testBoard, 0, false)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = i
                }
            }
        }
        return if (bestMove != -1) bestMove else currentBoard.indices.first { currentBoard[it] == null }
    }

    private fun minimax(boardList: List<String?>, depth: Int, isMaximizing: Boolean): Int {
        // Evaluate terminal states
        if (checkWin(boardList, "O") != null) return 10 - depth
        if (checkWin(boardList, "X") != null) return depth - 10
        if (boardList.none { it == null }) return 0

        if (isMaximizing) {
            var bestScore = Int.MIN_VALUE
            for (i in boardList.indices) {
                if (boardList[i] == null) {
                    val testBoard = boardList.toMutableList()
                    testBoard[i] = "O"
                    val score = minimax(testBoard, depth + 1, false)
                    bestScore = maxOf(bestScore, score)
                }
            }
            return bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for (i in boardList.indices) {
                if (boardList[i] == null) {
                    val testBoard = boardList.toMutableList()
                    testBoard[i] = "X"
                    val score = minimax(testBoard, depth + 1, true)
                    bestScore = minOf(bestScore, score)
                }
            }
            return bestScore
        }
    }

    private fun checkWin(boardList: List<String?>, player: String): List<Int>? {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
            listOf(0, 4, 8), listOf(2, 4, 6)                  // Diagonals
        )

        for (pattern in winPatterns) {
            if (boardList[pattern[0]] == player &&
                boardList[pattern[1]] == player &&
                boardList[pattern[2]] == player
            ) {
                return pattern
            }
        }
        return null
    }

    fun goToMenu() {
        _gameStatus.value = GameStatus.Menu
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun saveMatchToHistory(winnerSymbol: String) {
        viewModelScope.launch {
            val setup = _gameSetup.value
            val winnerName = when (winnerSymbol) {
                "X" -> setup.playerXName
                "O" -> setup.playerOName
                else -> "Draw"
            }

            val duration = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()

            val match = GameMatch(
                playerXName = setup.playerXName,
                playerOName = setup.playerOName,
                isVsAi = setup.isVsAi,
                aiDifficulty = if (setup.isVsAi) setup.aiDifficulty else "None",
                winner = winnerName,
                durationSeconds = duration,
                moveCount = moveCount
            )
            repository.insertMatch(match)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                val database = GameDatabase.getDatabase(application)
                val repository = GameMatchRepository(database.gameMatchDao())
                return GameViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
