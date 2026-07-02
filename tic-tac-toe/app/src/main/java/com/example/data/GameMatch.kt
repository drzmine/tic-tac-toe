package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_matches")
data class GameMatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerXName: String,
    val playerOName: String,
    val isVsAi: Boolean,
    val aiDifficulty: String, // "Easy", "Medium", "Hard", "None"
    val winner: String, // "X", "O", "Draw"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val moveCount: Int = 0
)
