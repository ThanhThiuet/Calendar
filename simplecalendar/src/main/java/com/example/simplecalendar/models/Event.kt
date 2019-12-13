package com.example.simplecalendar.models

import androidx.collection.LongSparseArray
import com.example.simplecalendar.extensions.seconds
import com.example.simplecalendar.helpers.*
import com.simplemobiletools.commons.extensions.addBitIf
import org.joda.time.DateTime
import java.io.Serializable

data class Event(
        var id: Long?,
        var startTS: Long = 0L,
        var endTS: Long = 0L,
        var title: String = "",
        var location: String = "",
        var description: String = "",
        var reminder1Minutes: Int = -1,
        var reminder2Minutes: Int = -1,
        var reminder3Minutes: Int = -1,
        var reminder1Type: Int = REMINDER_NOTIFICATION,
        var reminder2Type: Int = REMINDER_NOTIFICATION,
        var reminder3Type: Int = REMINDER_NOTIFICATION,
        var repeatInterval: Int = 0,
        var repeatRule: Int = 0,
        var repeatLimit: Long = 0L,
        var repetitionExceptions: ArrayList<String> = ArrayList(),
        var attendees: String = "",
        var importId: String = "",
        var flags: Int = 0,
        var eventType: Long = REGULAR_EVENT_TYPE_ID,
        var parentId: Long = 0,
        var lastUpdated: Long = 0L,
        var source: String = SOURCE_SIMPLE_CALENDAR)
    : Serializable {

    companion object {
        private const val serialVersionUID = -32456795132345616L
    }

    // if an event should happen on 31st with Same Day monthly repetition, dont show it at all at months with 30 or less days
    private fun addMonthsWithSameDay(currStart: DateTime, original: Event): DateTime {
        var newDateTime = currStart.plusMonths(repeatInterval / MONTH)
        if (newDateTime.dayOfMonth == currStart.dayOfMonth) {
            return newDateTime
        }

        while (newDateTime.dayOfMonth().maximumValue < Formatter.getDateTimeFromTS(original.startTS).dayOfMonth().maximumValue) {
            newDateTime = newDateTime.plusMonths(repeatInterval / MONTH)
            newDateTime = newDateTime.withDayOfMonth(currStart.dayOfMonth)
        }
        return newDateTime
    }

    // handle monthly repetitions like Third Monday
    private fun addXthDayInterval(currStart: DateTime, original: Event, forceLastWeekday: Boolean): DateTime {
        val day = currStart.dayOfWeek
        var order = (currStart.dayOfMonth - 1) / 7
        val properMonth = currStart.withDayOfMonth(7).plusMonths(repeatInterval / MONTH).withDayOfWeek(day)
        var firstProperDay = properMonth.dayOfMonth % 7
        if (firstProperDay == 0)
            firstProperDay = properMonth.dayOfMonth

        // check if it should be for example Fourth Monday, or Last Monday
        if (forceLastWeekday && (order == 3 || order == 4)) {
            val originalDateTime = Formatter.getDateTimeFromTS(original.startTS)
            val isLastWeekday = originalDateTime.monthOfYear != originalDateTime.plusDays(7).monthOfYear
            if (isLastWeekday)
                order = -1
        }

        val daysCnt = properMonth.dayOfMonth().maximumValue
        var wantedDay = firstProperDay + order * 7
        if (wantedDay > daysCnt)
            wantedDay -= 7

        if (order == -1) {
            wantedDay = firstProperDay + ((daysCnt - firstProperDay) / 7) * 7
        }

        return properMonth.withDayOfMonth(wantedDay)
    }

    fun getIsAllDay() = flags and FLAG_ALL_DAY != 0

    fun getReminders() = setOf(
            Reminder(reminder1Minutes, reminder1Type),
            Reminder(reminder2Minutes, reminder2Type),
            Reminder(reminder3Minutes, reminder3Type)
    ).filter { it.minutes != REMINDER_OFF }

    // properly return the start time of all-day events as midnight
    fun getEventStartTS(): Long {
        return if (getIsAllDay()) {
            Formatter.getDateTimeFromTS(startTS).withTime(0, 0, 0, 0).seconds()
        } else {
            startTS
        }
    }

    fun getCalDAVEventId(): Long {
        return try {
            (importId.split("-").lastOrNull() ?: "0").toString().toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    fun getCalDAVCalendarId() = if (source.startsWith(CALDAV)) (source.split("-").lastOrNull() ?: "0").toString().toInt() else 0

    fun updateIsPastEvent() {
        val endTSToCheck = if (startTS < getNowSeconds() && getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(endTS))
        } else {
            endTS
        }
        isPastEvent = endTSToCheck < getNowSeconds()
    }

    var isPastEvent: Boolean
        get() = flags and FLAG_IS_PAST_EVENT != 0
        set(isPastEvent) {
            flags = flags.addBitIf(isPastEvent, FLAG_IS_PAST_EVENT)
        }

    var color: Int = 0
}
