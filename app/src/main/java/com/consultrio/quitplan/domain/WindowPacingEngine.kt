package com.consultrio.quitplan.domain

import com.consultrio.quitplan.data.SmokingWindow
import java.time.*
import kotlin.math.max

object WindowPacingEngine {
    fun nextAllowedAt(lastStart:Instant, target:Int, wake:Int, sleep:Int, windows:List<SmokingWindow>, zone:ZoneId=ZoneId.systemDefault()):Instant {
        if(target<=0) return lastStart
        if(windows.isEmpty()) return SmokingEngine.nextAllowedAt(lastStart,PlanEngine.plannedIntervalMinutes(wake,sleep,target))
        val z=lastStart.atZone(zone); val minute=z.hour*60+z.minute
        val win=windows.firstOrNull{contains(it,minute)}
        if(win==null) return SmokingEngine.nextAllowedAt(lastStart,PlanEngine.plannedIntervalMinutes(wake,sleep,target))
        val awake=awakeMinutes(wake,sleep)
        val weighted=windows.sumOf{windowMinutes(it)*it.weight}.takeIf{it>0}?:awake.toDouble()
        val base=weighted/target
        val interval=max(10.0,base/win.weight).toLong()
        return lastStart.plus(Duration.ofMinutes(interval))
    }
    private fun contains(w:SmokingWindow,m:Int)=if(w.endMinute>w.startMinute)m in w.startMinute until w.endMinute else m>=w.startMinute||m<w.endMinute
    private fun windowMinutes(w:SmokingWindow)=if(w.endMinute>w.startMinute)w.endMinute-w.startMinute else 1440-w.startMinute+w.endMinute
    private fun awakeMinutes(w:Int,s:Int)=if(s>w)s-w else 1440-w+s
}
