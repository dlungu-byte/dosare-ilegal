package com.consultrio.quitplan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SmokingEventDao {
    @Insert suspend fun insert(event: SmokingEvent): Long
    @Query("DELETE FROM smoking_events WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("SELECT * FROM smoking_events WHERE estimatedStartAtMillis BETWEEN :start AND :end ORDER BY estimatedStartAtMillis ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<SmokingEvent>>
    @Query("SELECT COUNT(*) FROM smoking_events WHERE estimatedStartAtMillis BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int
    @Query("SELECT * FROM smoking_events ORDER BY estimatedStartAtMillis DESC LIMIT 1") suspend fun latest(): SmokingEvent?
}

@Dao
interface DailyPlanDao {
    @Upsert suspend fun upsert(plan: DailyPlan)
    @Query("SELECT * FROM daily_plans WHERE dateEpochDay = :epochDay LIMIT 1") suspend fun get(epochDay: Long): DailyPlan?
}
