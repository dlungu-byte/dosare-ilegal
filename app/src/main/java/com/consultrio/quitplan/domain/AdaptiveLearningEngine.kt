package com.consultrio.quitplan.domain

import com.consultrio.quitplan.data.SmokingEvent
import com.consultrio.quitplan.data.SmokingWindow
import java.time.*
import kotlin.math.max

object AdaptiveLearningEngine {
    private const val SLOT = 60
    fun isLearning(startEpochDay:Long, learningDays:Int, date:LocalDate=LocalDate.now()) = date.toEpochDay()-startEpochDay < learningDays
    fun reductionDayIndex(startEpochDay:Long,learningDays:Int,date:LocalDate=LocalDate.now()):Int = max(0,(date.toEpochDay()-startEpochDay-learningDays).toInt())
    fun learnedWindows(events:List<SmokingEvent>, forDate:LocalDate, wake:Int, sleep:Int, useWeekday:Boolean, zone:ZoneId=ZoneId.systemDefault()):List<SmokingWindow>{
        if(events.size<3) return emptyList()
        val general=DoubleArray(24); val specific=DoubleArray(24); var specificDays=mutableSetOf<LocalDate>()
        events.forEach { e -> val z=Instant.ofEpochMilli(e.estimatedStartAtMillis).atZone(zone); general[z.hour]+=1.0; if(z.dayOfWeek==forDate.dayOfWeek){specific[z.hour]+=1.0;specificDays.add(z.toLocalDate())} }
        val alpha=if(!useWeekday)0.0 else (specificDays.size/4.0).coerceIn(0.0,0.7)
        val scores=DoubleArray(24){h->general[h]*(1-alpha)+specific[h]*alpha}
        val active=(0..23).filter{h->inAwake(h*60,wake,sleep)}
        val avg=active.map{scores[it]}.average().takeIf{it>0}?:1.0
        return active.map{h->SmokingWindow(h*60,(h+1)*60,((scores[h]+avg*.35)/(avg*1.35)).coerceIn(.45,1.8))}
    }
    private fun inAwake(m:Int,w:Int,s:Int)=if(s>w)m in w until s else m>=w||m<s
}
