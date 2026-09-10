package com.consultrio.quitplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.consultrio.quitplan.data.*
import com.consultrio.quitplan.domain.PlanEngine
import com.consultrio.quitplan.domain.SmokingEngine
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val settings = UserSettings()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme {
            var smokedToday by remember { mutableIntStateOf(0) }; var nextAt by remember { mutableStateOf<Instant?>(null) }
            val target = settings.baselineCigarettesPerDay
            val interval = PlanEngine.plannedIntervalMinutes(settings.wakeMinuteOfDay, settings.sleepMinuteOfDay, target)
            LaunchedEffect(Unit) { smokedToday = countToday(); nextAt = DatabaseProvider.get(this@MainActivity).smokingEventDao().latest()?.let { SmokingEngine.nextAllowedAt(Instant.ofEpochMilli(it.estimatedStartAtMillis), interval) } }
            HomeScreen(smokedToday, target, nextAt) { action -> lifecycleScope.launch { register(action, interval); smokedToday = countToday(); nextAt = DatabaseProvider.get(this@MainActivity).smokingEventDao().latest()?.let { SmokingEngine.nextAllowedAt(Instant.ofEpochMilli(it.estimatedStartAtMillis), interval) } } }
        } }
    }
    private suspend fun register(action: SmokingActionType, interval: Int) {
        val pressed = Instant.now(); val estimated = SmokingEngine.estimatedStart(pressed, action, settings.cigaretteDurationMinutes)
        val latest = DatabaseProvider.get(this).smokingEventDao().latest(); val planned = latest?.let { SmokingEngine.nextAllowedAt(Instant.ofEpochMilli(it.estimatedStartAtMillis), interval) }
        DatabaseProvider.get(this).smokingEventDao().insert(SmokingEvent(pressedAtMillis=pressed.toEpochMilli(), estimatedStartAtMillis=estimated.toEpochMilli(), actionType=action, plannedAtMillis=planned?.toEpochMilli(), deviationMinutes=SmokingEngine.deviationMinutes(estimated, planned)))
    }
    private suspend fun countToday(): Int { val zone=ZoneId.systemDefault(); val start=LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli(); val end=LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()-1; return DatabaseProvider.get(this).smokingEventDao().countBetween(start,end) }
}

@Composable private fun HomeScreen(smokedToday:Int,target:Int,nextAt:Instant?,onAction:(SmokingActionType)->Unit) {
    val zone=ZoneId.systemDefault(); val nextText=nextAt?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm"))?:"—"; val remaining=nextAt?.let{Duration.between(Instant.now(),it).toMinutes()}?:0
    val status=if(nextAt==null) "Înregistrează prima țigară" else if(remaining>0) "Mai ai aproximativ $remaining min" else "Poți fuma acum"
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text("QuitPlan",style=MaterialTheme.typography.headlineMedium); Text("$smokedToday / $target azi",style=MaterialTheme.typography.headlineLarge); Text(status,style=MaterialTheme.typography.titleLarge); Text("Următoarea: $nextText")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ Button(onClick={onAction(SmokingActionType.LIGHT_NOW)}){Text("Aprind")}; Button(onClick={onAction(SmokingActionType.SMOKING_NOW)}){Text("Fumez")}; Button(onClick={onAction(SmokingActionType.SMOKED)}){Text("Am fumat")} }
    }
}
