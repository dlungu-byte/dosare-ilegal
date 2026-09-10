package com.consultrio.quitplan.domain

import kotlin.math.floor
import kotlin.math.max

object PlanEngine {
    fun targetForDay(baseline: Int, dayIndex: Int, reductionAmount: Int, reductionEveryDays: Int): Int {
        require(baseline >= 0 && dayIndex >= 0 && reductionAmount > 0 && reductionEveryDays > 0)
        val steps = floor(dayIndex.toDouble() / reductionEveryDays).toInt()
        return max(0, baseline - steps * reductionAmount)
    }
    fun buildTargets(baseline: Int, reductionAmount: Int, reductionEveryDays: Int): List<Int> {
        val result = mutableListOf<Int>(); var day = 0
        while (true) { val target = targetForDay(baseline, day, reductionAmount, reductionEveryDays); result += target; if (target == 0) break; day++ }
        return result
    }
    fun plannedIntervalMinutes(wakeMinute: Int, sleepMinute: Int, target: Int): Int {
        if (target <= 0) return 0
        val awake = if (sleepMinute > wakeMinute) sleepMinute - wakeMinute else (24 * 60 - wakeMinute) + sleepMinute
        return (awake / target).coerceAtLeast(1)
    }
}
