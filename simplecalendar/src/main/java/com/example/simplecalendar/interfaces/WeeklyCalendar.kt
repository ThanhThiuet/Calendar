package com.example.simplecalendar.interfaces

import com.example.simplecalendar.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
