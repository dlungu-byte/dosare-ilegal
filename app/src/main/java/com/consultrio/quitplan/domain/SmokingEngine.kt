package com.consultrio.quitplan.domain

import com.consultrio.quitplan.data.SmokingActionType
import java.time.Duration
import java.time.Instant

object SmokingEngine {
    fun estimatedStart(pressedAt: Instant, action: SmokingActionType, cigaretteDurationMinutes: Int = 5): Instant {
        val duration = Duration.ofMinutes(cigaretteDurationMinutes.toLong())
        return when (action) {
            SmokingActionType.LIGHT_NOW -> pressedAt
            SmokingActionType.SMOKING_NOW -> pressedAt.minus(duration.dividedBy(2))
            SmokingActionType.SMOKED -> pressedAt.minus(duration)
        }
    }
    fun nextAllowedAt(actualStart: Instant, plannedIntervalMinutes: Int): Instant = actualStart.plus(Duration.ofMinutes(plannedIntervalMinutes.toLong()))
    fun deviationMinutes(actualStart: Instant, plannedAt: Instant?): Int? = plannedAt?.let { Duration.between(it, actualStart).toMinutes().toInt() }
}
