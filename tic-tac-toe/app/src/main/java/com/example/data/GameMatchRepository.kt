package com.example.data

import kotlinx.coroutines.flow.Flow

class GameMatchRepository(private val gameMatchDao: GameMatchDao) {
    val allMatches: Flow<List<GameMatch>> = gameMatchDao.getAllMatches()

    suspend fun insertMatch(match: GameMatch) {
        gameMatchDao.insertMatch(match)
    }

    suspend fun clearHistory() {
        gameMatchDao.clearHistory()
    }
}
