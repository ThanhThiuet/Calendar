package com.example.simplecalendar.helpers

import android.content.Context
import com.example.simplecalendar.extensions.eventsHelper
import com.example.simplecalendar.interfaces.WeeklyCalendar
import com.example.simplecalendar.models.Event
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import java.util.*

class WeeklyCalendarImpl(val callback: WeeklyCalendar, val context: Context) {
    var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Long) {
        val endTS = weekStartTS + WEEK_SECONDS
        context.eventsHelper.getEvents(weekStartTS, endTS) {
            mEvents = it
            callback.updateWeeklyCalendar(it)
        }
    }
}
