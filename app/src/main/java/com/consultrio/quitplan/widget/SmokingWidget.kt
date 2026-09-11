package com.consultrio.quitplan.widget
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.consultrio.quitplan.MainActivity
import com.consultrio.quitplan.data.*
import com.consultrio.quitplan.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.max

class SmokingWidget:GlanceAppWidget(){
 override suspend fun provideGlance(c:Context,id:GlanceId){
  val s=SettingsStore(c).load(); val z=ZoneId.systemDefault(); val d=LocalDate.now(z)
  val learn=s.adaptiveWindows&&AdaptiveLearningEngine.isLearning(s.planStartEpochDay,s.learningDays,d)
  val idx=if(s.adaptiveWindows)AdaptiveLearningEngine.reductionDayIndex(s.planStartEpochDay,s.learningDays,d) else max(0,(d.toEpochDay()-s.planStartEpochDay).toInt())
  val t=if(learn)s.baseline else PlanEngine.targetForDay(s.baseline,idx,s.reductionAmount,s.reductionEveryDays)
  val dao=DatabaseProvider.get(c).smokingEventDao(); val start=d.atStartOfDay(z).toInstant().toEpochMilli(); val count=dao.countBetween(start,d.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()-1); val last=dao.latest()
  val events=dao.eventsSince(LocalDate.ofEpochDay(s.planStartEpochDay).atStartOfDay(z).toInstant().toEpochMilli())
  val wins=if(s.adaptiveWindows&&!learn)AdaptiveLearningEngine.learnedWindows(events,d,s.wakeMinute,s.sleepMinute,s.weekdayLearning,z).ifEmpty{s.windows}else s.windows
  val next=last?.takeIf{t>0}?.let{val i=Instant.ofEpochMilli(it.estimatedStartAtMillis);if(s.adaptiveWindows&&!learn)WindowPacingEngine.nextAllowedAt(i,t,s.wakeMinute,s.sleepMinute,wins,z)else SmokingEngine.nextAllowedAt(i,PlanEngine.plannedIntervalMinutes(s.wakeMinute,s.sleepMinute,t))}
  val now=Instant.now(); val status=when{learn->"Învățare ${((d.toEpochDay()-s.planStartEpochDay+1).toInt()).coerceAtMost(s.learningDays)}/${s.learningDays}";t<=0->"Ținta de azi: 0";next==null->"Înregistrează prima țigară";next.isAfter(now)->"Următoarea în ${Duration.between(now,next).toMinutes()+1} min";else->"Poți fuma · amânat ${Duration.between(next,now).toMinutes()} min"}
  val manualIntent=Intent(c,MainActivity::class.java).putExtra("manual_add",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  provideContent{
   Column(GlanceModifier.fillMaxSize().background(ColorProvider(Color.White)).cornerRadius(android.R.dimen.system_app_widget_background_radius).padding(14.dp)){
    Row(GlanceModifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Text("QuitPlan",style=TextStyle(color=ColorProvider(Color(0xFF0F172A))));Spacer(GlanceModifier.width(10.dp));Text("$count / $t azi",style=TextStyle(color=ColorProvider(Color(0xFF166534))))}
    Spacer(GlanceModifier.height(4.dp));Text(status,style=TextStyle(color=ColorProvider(Color(0xFF334155))))
    next?.let{Text("Ora planificată: "+it.atZone(z).format(DateTimeFormatter.ofPattern("HH:mm")),style=TextStyle(color=ColorProvider(Color(0xFF64748B))))}
    Spacer(GlanceModifier.height(10.dp))
    Row(GlanceModifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Btn("APRIND",Color(0xFFF97316),actionRunCallback<LightNow>());Spacer(GlanceModifier.width(7.dp));Btn("FUMEZ",Color(0xFF2563EB),actionRunCallback<SmokingNow>())}
    Spacer(GlanceModifier.height(7.dp))
    Row(GlanceModifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Btn("AM FUMAT",Color(0xFF16A34A),actionRunCallback<Smoked>());Spacer(GlanceModifier.width(7.dp));Btn("+ ADAUGĂ",Color(0xFF7C3AED),actionStartActivity(manualIntent))}
   }
  }
 }
 @androidx.compose.runtime.Composable private fun Btn(x:String,color:Color,a:Action){Box(GlanceModifier.width(132.dp).height(48.dp).background(ColorProvider(color)).cornerRadius(14.dp).clickable(a),contentAlignment=Alignment.Center){Text(x,style=TextStyle(color=ColorProvider(Color.White)))}}
}
class SmokingWidgetReceiver:GlanceAppWidgetReceiver(){override val glanceAppWidget=SmokingWidget()}
private suspend fun record(c:Context,a:SmokingActionType){val s=SettingsStore(c).load();val z=ZoneId.systemDefault();val d=LocalDate.now(z);val learn=s.adaptiveWindows&&AdaptiveLearningEngine.isLearning(s.planStartEpochDay,s.learningDays,d);val idx=if(s.adaptiveWindows)AdaptiveLearningEngine.reductionDayIndex(s.planStartEpochDay,s.learningDays,d)else max(0,(d.toEpochDay()-s.planStartEpochDay).toInt());val t=if(learn)s.baseline else PlanEngine.targetForDay(s.baseline,idx,s.reductionAmount,s.reductionEveryDays);if(t<=0)return;val dao=DatabaseProvider.get(c).smokingEventDao();val p=Instant.now();val est=SmokingEngine.estimatedStart(p,a,s.cigaretteDuration);val last=dao.latest();val events=dao.eventsSince(LocalDate.ofEpochDay(s.planStartEpochDay).atStartOfDay(z).toInstant().toEpochMilli());val wins=AdaptiveLearningEngine.learnedWindows(events,d,s.wakeMinute,s.sleepMinute,s.weekdayLearning,z).ifEmpty{s.windows};val planned=last?.let{val i=Instant.ofEpochMilli(it.estimatedStartAtMillis);if(s.adaptiveWindows&&!learn)WindowPacingEngine.nextAllowedAt(i,t,s.wakeMinute,s.sleepMinute,wins,z)else SmokingEngine.nextAllowedAt(i,PlanEngine.plannedIntervalMinutes(s.wakeMinute,s.sleepMinute,t))};dao.insert(SmokingEvent(pressedAtMillis=p.toEpochMilli(),estimatedStartAtMillis=est.toEpochMilli(),actionType=a,plannedAtMillis=planned?.toEpochMilli(),deviationMinutes=SmokingEngine.deviationMinutes(est,planned)));SmokingWidget().updateAll(c)}
class LightNow:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.LIGHT_NOW)}
class SmokingNow:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.SMOKING_NOW)}
class Smoked:ActionCallback{override suspend fun onAction(context:Context,glanceId:GlanceId,parameters:ActionParameters)=record(context,SmokingActionType.SMOKED)}
