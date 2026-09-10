package com.consultrio.quitplan.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.consultrio.quitplan.R
import com.consultrio.quitplan.data.*
import com.consultrio.quitplan.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.max

class SmokingWidget : GlanceAppWidget() {
 override suspend fun provideGlance(context:Context,id:GlanceId){
  val s=SettingsStore(context).load();val zone=ZoneId.systemDefault();val today=LocalDate.now(zone);val day=max(0,(today.toEpochDay()-s.planStartEpochDay).toInt());val target=PlanEngine.targetForDay(s.baseline,day,s.reductionAmount,s.reductionEveryDays)
  val start=today.atStartOfDay(zone).toInstant().toEpochMilli();val end=today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-1;val dao=DatabaseProvider.get(context).smokingEventDao();val count=dao.countBetween(start,end);val latest=dao.latest()
  val next=if(latest!=null&&target>0){val last=Instant.ofEpochMilli(latest.estimatedStartAtMillis);if(s.adaptiveWindows)WindowPacingEngine.nextAllowedAt(last,target,s.wakeMinute,s.sleepMinute,s.windows,zone) else SmokingEngine.nextAllowedAt(last,PlanEngine.plannedIntervalMinutes(s.wakeMinute,s.sleepMinute,target))}else null
  val now=Instant.now();val status=when{target<=0->"Ținta de azi: 0";next==null->"Înregistrează prima țigară";next.isAfter(now)->"Următoarea în ${Duration.between(now,next).toMinutes()+1} min";else->"Poți fuma · amânat ${Duration.between(next,now).toMinutes()} min"};val clock=next?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm"))
  provideContent{Column(GlanceModifier.fillMaxSize().padding(12.dp)){Text("QuitPlan · $count / $target azi",style=TextStyle(color=ColorProvider(Color.Black)));Text(status);if(clock!=null&&target>0)Text("Ora planificată: $clock");Spacer(GlanceModifier.height(10.dp));Row(GlanceModifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Button("APRIND",actionRunCallback<LightNow>());Spacer(GlanceModifier.width(6.dp));Button("FUMEZ",actionRunCallback<SmokingNow>());Spacer(GlanceModifier.width(6.dp));Button("AM FUMAT",actionRunCallback<Smoked>())}}}
 }
 @androidx.glance.appwidget.components.ExperimentalGlanceComponentsApi
 @androidx.compose.runtime.Composable private fun Button(label:String,action:androidx.glance.action.Action){androidx.glance.appwidget.components.Button(text=label,onClick=action,modifier=GlanceModifier.defaultWeight().height(52.dp))}
}
class SmokingWidgetReceiver:GlanceAppWidgetReceiver(){override val glanceAppWidget:GlanceAppWidget=SmokingWidget()}
private suspend fun record(context:Context,action:SmokingActionType){val s=SettingsStore(context).load();val zone=ZoneId.systemDefault();val today=LocalDate.now(zone);val day=max(0,(today.toEpochDay()-s.planStartEpochDay).toInt());val target=PlanEngine.targetForDay(s.baseline,day,s.reductionAmount,s.reductionEveryDays);if(target<=0){SmokingWidget().updateAll(context);return};val pressed=Instant.now();val estimated=SmokingEngine.estimatedStart(pressed,action,s.cigaretteDuration);val dao=DatabaseProvider.get(context).smokingEventDao();val latest=dao.latest();val planned=latest?.let{val last=Instant.ofEpochMilli(it.estimatedStartAtMillis);if(s.adaptiveWindows)WindowPacingEngine.nextAllowedAt(last,target,s.wakeMinute,s.sleepMinute,s.windows,zone)else SmokingEngine.nextAllowedAt(last,PlanEngine.plannedIntervalMinutes(s.wakeMinute,s.sleepMinute,target))};dao.insert(SmokingEvent(pressedAtMillis=pressed.toEpochMilli(),estimatedStartAtMillis=estimated.toEpochMilli(),actionType=action,plannedAtMillis=planned?.toEpochMilli(),deviationMinutes=SmokingEngine.deviationMinutes(estimated,planned)));SmokingWidget().updateAll(context)}
class LightNow:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.LIGHT_NOW)}
class SmokingNow:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.SMOKING_NOW)}
class Smoked:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.SMOKED)}
