package com.consultrio.quitplan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SmokingActionType { LIGHT_NOW, SMOKING_NOW, SMOKED }

@Entity(tableName = "smoking_events")
data class SmokingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pressedAtMillis: Long,
    val estimatedStartAtMillis: Long,
    val actionType: SmokingActionType,
    val plannedAtMillis: Long?,
    val deviationMinutes: Int?
)

@Entity(tableName = "daily_plans")
data class DailyPlan(
    @PrimaryKey val dateEpochDay: Long,
    val targetCigarettes: Int,
    val plannedIntervalMinutes: Int,
    val wakeMinuteOfDay: Int,
    val sleepMinuteOfDay: Int
)

data class UserSettings(
    val baselineCigarettesPerDay: Int = 20,
    val wakeMinuteOfDay: Int = 7 * 60,
    val sleepMinuteOfDay: Int = 23 * 60,
    val cigaretteDurationMinutes: Int = 5,
    val packPrice: Double = 30.0,
    val cigarettesPerPack: Int = 20,
    val reductionAmount: Int = 1,
    val reductionEveryDays: Int = 1
)
