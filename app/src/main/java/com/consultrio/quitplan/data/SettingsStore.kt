package com.consultrio.quitplan.data

import android.content.Context
import java.time.LocalDate

data class SmokingWindow(val startMinute:Int,val endMinute:Int,val weight:Double)

data class StoredSettings(
    val baseline: Int = 20,
    val wakeMinute: Int = 7 * 60,
    val sleepMinute: Int = 23 * 60,
    val cigaretteDuration: Int = 5,
    val packPrice: Double = 30.0,
    val packSize: Int = 20,
    val reductionAmount: Int = 1,
    val reductionEveryDays: Int = 1,
    val planStartEpochDay: Long = LocalDate.now().toEpochDay(),
    val packsOwned: Double = 0.0,
    val onboardingDone: Boolean = false,
    val adaptiveWindows: Boolean = false,
    val windows: List<SmokingWindow> = emptyList()
)

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("quitplan_settings", Context.MODE_PRIVATE)
    fun load(): StoredSettings = StoredSettings(
        baseline=p.getInt("baseline",20), wakeMinute=p.getInt("wake",420), sleepMinute=p.getInt("sleep",1380),
        cigaretteDuration=p.getInt("duration",5), packPrice=java.lang.Double.longBitsToDouble(p.getLong("price",java.lang.Double.doubleToRawLongBits(30.0))),
        packSize=p.getInt("packSize",20), reductionAmount=p.getInt("reduction",1), reductionEveryDays=p.getInt("every",1),
        planStartEpochDay=p.getLong("start",LocalDate.now().toEpochDay()),
        packsOwned=java.lang.Double.longBitsToDouble(p.getLong("owned",java.lang.Double.doubleToRawLongBits(0.0))), onboardingDone=p.getBoolean("done",false),
        adaptiveWindows=p.getBoolean("adaptiveWindows",false), windows=decodeWindows(p.getString("windows","").orEmpty())
    )
    fun save(s: StoredSettings) { p.edit().putInt("baseline",s.baseline).putInt("wake",s.wakeMinute).putInt("sleep",s.sleepMinute)
        .putInt("duration",s.cigaretteDuration).putLong("price",java.lang.Double.doubleToRawLongBits(s.packPrice)).putInt("packSize",s.packSize)
        .putInt("reduction",s.reductionAmount).putInt("every",s.reductionEveryDays).putLong("start",s.planStartEpochDay)
        .putLong("owned",java.lang.Double.doubleToRawLongBits(s.packsOwned)).putBoolean("done",s.onboardingDone)
        .putBoolean("adaptiveWindows",s.adaptiveWindows).putString("windows",encodeWindows(s.windows)).apply() }
    private fun encodeWindows(w:List<SmokingWindow>)=w.joinToString(";"){"${it.startMinute},${it.endMinute},${it.weight}"}
    private fun decodeWindows(raw:String)=raw.split(';').mapNotNull{v->val a=v.split(',');if(a.size!=3)null else {val s=a[0].toIntOrNull();val e=a[1].toIntOrNull();val w=a[2].toDoubleOrNull();if(s==null||e==null||w==null)null else SmokingWindow(s,e,w)}}
}
