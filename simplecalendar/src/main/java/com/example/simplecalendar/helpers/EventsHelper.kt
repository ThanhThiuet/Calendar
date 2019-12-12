package com.example.simplecalendar.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.collection.LongSparseArray
import com.example.simplecalendar.R
import com.example.simplecalendar.extensions.*
import com.example.simplecalendar.models.Event
import com.example.simplecalendar.models.EventType
import com.simplemobiletools.commons.extensions.getChoppedList
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

class EventsHelper(val context: Context) {
    private val config = context.config

    companion object {
        var eventTest: Event = Event(null)
    }

    fun getEvents(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean = true, callback: (events: ArrayList<Event>) -> Unit) {
        ensureBackgroundThread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, callback)
        }
    }

    @SuppressLint("LongLogTag")
    fun getEventsSync(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean, callback: (events: ArrayList<Event>) -> Unit) {
        val fromDate: DateTime = Formatter.getDateTimeFromTS(fromTS)
        val toDate: DateTime = Formatter.getDateTimeFromTS(toTS)
        Log.e("EventsHelper.class", "fromDate: $fromDate")
        Log.e("EventsHelper.class", "toDate: $toDate")

        val events: ArrayList<Event> = ArrayList()

        val newImportId = "b08747857cb44e1b879c837b9e016e091575271897414" /*""*/ /*UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis().toString()*/
        val currentTimestamp = System.currentTimeMillis()
        val listString: ArrayList<String> = ArrayList()
        val date: DateTime = Formatter.getDateTimeFromTS(currentTimestamp / 1000L)
        val startDate: DateTime = date.withDate(2019, 12, 10).withHourOfDay(7).withMinuteOfHour(0)
        val endDate: DateTime = date.withDate(2019, 12, 12).withHourOfDay(7).withMinuteOfHour(0)

        eventTest = Event(/*REGULAR_EVENT_TYPE_ID*/12, startDate.seconds(), endDate.seconds(), "đây là title", "location", "des", -1, -1, -1, 0, 0, 0, 0, 0 ,0 , listString, "",  newImportId, 0, 1, 0, currentTimestamp , SOURCE_SIMPLE_CALENDAR)
        eventTest.updateIsPastEvent()
        eventTest.color = config.primaryColor
        Log.e("EventsHelper.class_addEventTest", "" + eventTest.getCalDAVCalendarId() + " - " + eventTest.getCalDAVEventId() + " - " + eventTest.id + " - " + eventTest.getEventStartTS() + " - " + eventTest.startTS + " - " + eventTest.eventType + " - " + eventTest.importId)

        config.displayEventTypes
        events.add(eventTest)

        callback(events)
    }
}
