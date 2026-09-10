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
import com.consultrio.quitplan.data.*
import com.consultrio.quitplan.domain.PlanEngine
import com.consultrio.quitplan.domain.SmokingEngine
import java.time.*

class SmokingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val zone=ZoneId.systemDefault(); val start=LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli(); val end=LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-1
        val count=DatabaseProvider.get(context).smokingEventDao().countBetween(start,end)
        provideContent { Column(GlanceModifier.padding(12.dp)){ Text("QuitPlan · $count / 20 azi"); Row { Text("Aprind",GlanceModifier.padding(8.dp).clickable(actionRunCallback<LightNow>())); Text("Fumez",GlanceModifier.padding(8.dp).clickable(actionRunCallback<SmokingNow>())); Text("Am fumat",GlanceModifier.padding(8.dp).clickable(actionRunCallback<Smoked>())) } } }
    }
}
class SmokingWidgetReceiver:GlanceAppWidgetReceiver(){ override val glanceAppWidget:GlanceAppWidget=SmokingWidget() }
private suspend fun record(context:Context,action:SmokingActionType){ val s=UserSettings(); val interval=PlanEngine.plannedIntervalMinutes(s.wakeMinuteOfDay,s.sleepMinuteOfDay,s.baselineCigarettesPerDay); val pressed=Instant.now(); val estimated=SmokingEngine.estimatedStart(pressed,action,s.cigaretteDurationMinutes); val latest=DatabaseProvider.get(context).smokingEventDao().latest(); val planned=latest?.let{SmokingEngine.nextAllowedAt(Instant.ofEpochMilli(it.estimatedStartAtMillis),interval)}; DatabaseProvider.get(context).smokingEventDao().insert(SmokingEvent(pressedAtMillis=pressed.toEpochMilli(),estimatedStartAtMillis=estimated.toEpochMilli(),actionType=action,plannedAtMillis=planned?.toEpochMilli(),deviationMinutes=SmokingEngine.deviationMinutes(estimated,planned))); SmokingWidget().updateAll(context) }
class LightNow:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.LIGHT_NOW)}
class SmokingNow:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.SMOKING_NOW)}
class Smoked:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.SMOKED)}
