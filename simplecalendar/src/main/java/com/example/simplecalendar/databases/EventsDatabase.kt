package com.example.simplecalendar.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.simplecalendar.R
import com.example.simplecalendar.extensions.config
import com.example.simplecalendar.helpers.Converters
import com.example.simplecalendar.helpers.REGULAR_EVENT_TYPE_ID
import com.example.simplecalendar.interfaces.EventTypesDao
import com.example.simplecalendar.interfaces.EventsDao
import com.example.simplecalendar.models.Event
import com.example.simplecalendar.models.EventType
import java.util.concurrent.Executors

@Database(entities = [Event::class, EventType::class], version = 2)
@TypeConverters(Converters::class)
abstract class EventsDatabase : RoomDatabase() {

    abstract fun EventsDao(): EventsDao

    abstract fun EventTypesDao(): EventTypesDao

    companion object {
        private var db: EventsDatabase? = null

        fun getInstance(context: Context): EventsDatabase {
            if (db == null) {
                synchronized(EventsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, EventsDatabase::class.java, "events.db")
                                .addCallback(object : Callback() {
                                    override fun onCreate(db: SupportSQLiteDatabase) {
                                        super.onCreate(db)
                                        insertRegularEventType(context)
                                    }
                                })
                                .addMigrations(MIGRATION_1_2)
                                .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        private fun insertRegularEventType(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val regularEvent = context.resources.getString(R.string.regular_event)
                val eventType = EventType(REGULAR_EVENT_TYPE_ID, regularEvent, context.config.primaryColor)
                db!!.EventTypesDao().insertOrUpdate(eventType)
                context.config.addDisplayEventType(REGULAR_EVENT_TYPE_ID.toString())
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN reminder_1_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN reminder_2_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN reminder_3_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN attendees TEXT NOT NULL DEFAULT ''")
                }
            }
        }
    }
}
