package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameMatchDao {
    @Query("SELECT * FROM game_matches ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<GameMatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: GameMatch)

    @Query("DELETE FROM game_matches")
    suspend fun clearHistory()
}
