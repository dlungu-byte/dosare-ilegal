package com.consultrio.quitplan.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromAction(value: SmokingActionType): String = value.name
    @TypeConverter fun toAction(value: String): SmokingActionType = SmokingActionType.valueOf(value)
}

@Database(entities = [SmokingEvent::class, DailyPlan::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokingEventDao(): SmokingEventDao
    abstract fun dailyPlanDao(): DailyPlanDao
}
