package com.consultrio.quitplan.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.consultrio.quitplan.data.DatabaseProvider
import com.consultrio.quitplan.data.SettingsStore
import com.consultrio.quitplan.data.SmokingActionType
import com.consultrio.quitplan.data.SmokingEvent
import com.consultrio.quitplan.domain.PlanEngine
import com.consultrio.quitplan.domain.SmokingEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

class SmokingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsStore(context).load()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dayIndex = max(0, (today.toEpochDay() - settings.planStartEpochDay).toInt())
        val target = PlanEngine.targetForDay(
            settings.baseline,
            dayIndex,
            settings.reductionAmount,
            settings.reductionEveryDays
        )
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val dao = DatabaseProvider.get(context).smokingEventDao()
        val count = dao.countBetween(start, end)
        val latest = dao.latest()
        val interval = PlanEngine.plannedIntervalMinutes(settings.wakeMinute, settings.sleepMinute, target)
        val next = if (latest != null && target > 0) {
            SmokingEngine.nextAllowedAt(Instant.ofEpochMilli(latest.estimatedStartAtMillis), interval)
        } else null
        val now = Instant.now()
        val status = when {
            target <= 0 -> "Ținta de azi: 0"
            next == null -> "Înregistrează prima țigară"
            next.isAfter(now) -> {
                val minutes = java.time.Duration.between(now, next).toMinutes() + 1
                "Următoarea în $minutes min"
            }
            else -> {
                val delayed = java.time.Duration.between(next, now).toMinutes()
                "Poți fuma · amânat $delayed min"
            }
        }
        val nextClock = next?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm"))

        provideContent {
            Column(GlanceModifier.padding(12.dp)) {
                Text("QuitPlan · $count / $target azi")
                Text(status)
                if (nextClock != null && target > 0) Text("Ora planificată: $nextClock")
                Row {
                    Text("Aprind", GlanceModifier.padding(8.dp).clickable(actionRunCallback<LightNow>()))
                    Text("Fumez", GlanceModifier.padding(8.dp).clickable(actionRunCallback<SmokingNow>()))
                    Text("Am fumat", GlanceModifier.padding(8.dp).clickable(actionRunCallback<Smoked>()))
                }
            }
        }
    }
}

class SmokingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmokingWidget()
}

private suspend fun record(context: Context, action: SmokingActionType) {
    val settings = SettingsStore(context).load()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val dayIndex = max(0, (today.toEpochDay() - settings.planStartEpochDay).toInt())
    val target = PlanEngine.targetForDay(
        settings.baseline,
        dayIndex,
        settings.reductionAmount,
        settings.reductionEveryDays
    )
    if (target <= 0) {
        SmokingWidget().updateAll(context)
        return
    }

    val interval = PlanEngine.plannedIntervalMinutes(settings.wakeMinute, settings.sleepMinute, target)
    val pressed = Instant.now()
    val estimated = SmokingEngine.estimatedStart(pressed, action, settings.cigaretteDuration)
    val dao = DatabaseProvider.get(context).smokingEventDao()
    val latest = dao.latest()
    val planned = latest?.let {
        SmokingEngine.nextAllowedAt(Instant.ofEpochMilli(it.estimatedStartAtMillis), interval)
    }
    dao.insert(
        SmokingEvent(
            pressedAtMillis = pressed.toEpochMilli(),
            estimatedStartAtMillis = estimated.toEpochMilli(),
            actionType = action,
            plannedAtMillis = planned?.toEpochMilli(),
            deviationMinutes = SmokingEngine.deviationMinutes(estimated, planned)
        )
    )
    SmokingWidget().updateAll(context)
}

class LightNow : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        record(context, SmokingActionType.LIGHT_NOW)
}

class SmokingNow : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        record(context, SmokingActionType.SMOKING_NOW)
}

class Smoked : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        record(context, SmokingActionType.SMOKED)
}
