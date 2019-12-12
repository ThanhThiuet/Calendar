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
//    private val eventsDB = context.eventsDB
//    private val eventTypesDB = context.eventTypesDB
    companion object {
        var eventTest: Event = Event(null)
    }

    fun getEventTypes(activity: Activity, showWritableOnly: Boolean, callback: (notes: ArrayList<EventType>) -> Unit) {
        ensureBackgroundThread {
            var eventTypes = ArrayList<EventType>()
            try {
//                eventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            } catch (ignored: Exception) {
            }

            if (showWritableOnly) {
                val caldavCalendars = activity.calDAVHelper.getCalDAVCalendars("", true)
                eventTypes = eventTypes.filter {
                    val eventType = it
                    it.caldavCalendarId == 0 || caldavCalendars.firstOrNull { it.id == eventType.caldavCalendarId }?.canWrite() == true
                }.toMutableList() as ArrayList<EventType>
            }

            activity.runOnUiThread {
                callback(eventTypes)
            }
        }
    }

//    fun getEventTypesSync() = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>

//    fun insertOrUpdateEventType(activity: Activity, eventType: EventType, callback: ((newEventTypeId: Long) -> Unit)? = null) {
//        ensureBackgroundThread {
//            val eventTypeId = insertOrUpdateEventTypeSync(eventType)
//            activity.runOnUiThread {
//                callback?.invoke(eventTypeId)
//            }
//        }
//    }

//    fun insertOrUpdateEventTypeSync(eventType: EventType): Long {
//        if (eventType.id != null && eventType.id!! > 0 && eventType.caldavCalendarId != 0) {
//            context.calDAVHelper.updateCalDAVCalendar(eventType)
//        }
//
//        val newId = eventTypesDB.insertOrUpdate(eventType)
//        if (eventType.id == null) {
//            config.addDisplayEventType(newId.toString())
//        }
//        return newId
//    }

//    fun getEventTypeIdWithTitle(title: String) = eventTypesDB.getEventTypeIdWithTitle(title) ?: -1L
//
//    fun getEventTypeWithCalDAVCalendarId(calendarId: Int) = eventTypesDB.getEventTypeWithCalDAVCalendarId(calendarId)

//    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean) {
//        val typesToDelete = eventTypes.asSequence().filter { it.caldavCalendarId == 0 && it.id != REGULAR_EVENT_TYPE_ID }.toMutableList()
//        val deleteIds = typesToDelete.map { it.id }.toMutableList()
//        val deletedSet = deleteIds.map { it.toString() }.toHashSet()
//        config.removeDisplayEventTypes(deletedSet)
//
//        if (deleteIds.isEmpty()) {
//            return
//        }
//
//        for (eventTypeId in deleteIds) {
//            if (deleteEvents) {
//                deleteEventsWithType(eventTypeId!!)
//            } else {
//                eventsDB.resetEventsWithType(eventTypeId!!)
//            }
//        }
//
//        eventTypesDB.deleteEventTypes(typesToDelete)
//    }
//
//    fun insertEvent(event: Event, addToCalDAV: Boolean, showToasts: Boolean, callback: ((id: Long) -> Unit)? = null) {
//        if (event.startTS > event.endTS) {
//            callback?.invoke(0)
//            return
//        }
//
//        event.id = eventsDB.insertOrUpdate(event)
//
//        context.updateWidgets()
//
//        if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
//            context.calDAVHelper.insertCalDAVEvent(event)
//        }
//
//        callback?.invoke(event.id!!)
//    }
//
//    fun insertEvents(events: ArrayList<Event>, addToCalDAV: Boolean) {
//        try {
//            for (event in events) {
//                if (event.startTS > event.endTS) {
//                    continue
//                }
//
//                event.id = eventsDB.insertOrUpdate(event)
//
//                if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS && config.caldavSync) {
//                    context.calDAVHelper.insertCalDAVEvent(event)
//                }
//            }
//        } finally {
//            context.updateWidgets()
//        }
//    }
//
//    fun updateEvent(event: Event, updateAtCalDAV: Boolean, showToasts: Boolean, callback: (() -> Unit)? = null) {
//        eventsDB.insertOrUpdate(event)
//
//        context.updateWidgets()
//        if (updateAtCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
//            context.calDAVHelper.updateCalDAVEvent(event)
//        }
//        callback?.invoke()
//    }
//
//    fun deleteAllEvents() {
//        ensureBackgroundThread {
//            val eventIds = eventsDB.getEventIds().toMutableList()
//            deleteEvents(eventIds, true)
//        }
//    }
//
//    fun deleteEvent(id: Long, deleteFromCalDAV: Boolean) = deleteEvents(arrayListOf(id), deleteFromCalDAV)
//
//    fun deleteEvents(ids: MutableList<Long>, deleteFromCalDAV: Boolean) {
//        if (ids.isEmpty()) {
//            return
//        }
//
//        ids.getChoppedList().forEach {
//            val eventsWithImportId = eventsDB.getEventsByIdsWithImportIds(it)
//            eventsDB.deleteEvents(it)
//
//            if (deleteFromCalDAV && config.caldavSync) {
//                eventsWithImportId.forEach {
//                    context.calDAVHelper.deleteCalDAVEvent(it)
//                }
//            }
//
//            deleteChildEvents(it, deleteFromCalDAV)
//            context.updateWidgets()
//        }
//    }
//
//    private fun deleteChildEvents(ids: MutableList<Long>, deleteFromCalDAV: Boolean) {
//        val childIds = eventsDB.getEventIdsWithParentIds(ids).toMutableList()
//        if (childIds.isNotEmpty()) {
//            deleteEvents(childIds, deleteFromCalDAV)
//        }
//    }
//
//    private fun deleteEventsWithType(eventTypeId: Long) {
//        val eventIds = eventsDB.getEventIdsByEventType(eventTypeId).toMutableList()
//        deleteEvents(eventIds, true)
//    }
//
//    fun addEventRepeatLimit(eventId: Long, limitTS: Long) {
//        val time = Formatter.getDateTimeFromTS(limitTS)
//        eventsDB.updateEventRepetitionLimit(limitTS - time.hourOfDay, eventId)
//
//        if (config.caldavSync) {
//            val event = eventsDB.getEventWithId(eventId)
//            if (event?.getCalDAVCalendarId() != 0) {
//                context.calDAVHelper.updateCalDAVEvent(event!!)
//            }
//        }
//    }
//
//    fun doEventTypesContainEvents(eventTypeIds: ArrayList<Long>, callback: (contain: Boolean) -> Unit) {
//        ensureBackgroundThread {
//            val eventIds = eventsDB.getEventIdsByEventType(eventTypeIds)
//            callback(eventIds.isNotEmpty())
//        }
//    }

//    fun getEventsWithSearchQuery(text: String, activity: Activity, callback: (searchedText: String, events: List<Event>) -> Unit) {
//        ensureBackgroundThread {
//            val searchQuery = "%$text%"
//            val events = eventsDB.getEventsForSearch(searchQuery)
//            val displayEventTypes = config.displayEventTypes
//            val filteredEvents = events.filter { displayEventTypes.contains(it.eventType.toString()) }
//            activity.runOnUiThread {
//                callback(text, filteredEvents)
//            }
//        }
//    }

//    fun addEventRepetitionException(parentEventId: Long, occurrenceTS: Long, addToCalDAV: Boolean) {
//        ensureBackgroundThread {
//            val parentEvent = eventsDB.getEventWithId(parentEventId) ?: return@ensureBackgroundThread
//
//            if (addToCalDAV && config.caldavSync) {
//                context.calDAVHelper.insertEventRepeatException(parentEvent, occurrenceTS)
//            }
//        }
//    }

    fun getEvents(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean = true, callback: (events: ArrayList<Event>) -> Unit) {
        ensureBackgroundThread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, callback)
        }
    }

    @SuppressLint("LongLogTag")
    fun getEventsSync(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean, callback: (events: ArrayList<Event>) -> Unit) {
        val events: ArrayList<Event> = ArrayList()
//        var events = if (applyTypeFilter) {
//            val displayEventTypes = context.config.displayEventTypes
//            if (displayEventTypes.isEmpty()) {
//                callback(ArrayList())
//                return
//            } else {
////                eventsDB.getOneTimeEventsFromToWithTypes(toTS, fromTS, context.config.getDisplayEventTypessAsList()).toMutableList() as ArrayList<Event>
//            }
//        } else {
//            if (eventId == -1L) {
////                eventsDB.getOneTimeEventsFromTo(toTS, fromTS).toMutableList() as ArrayList<Event>
//            } else {
////                eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS).toMutableList() as ArrayList<Event>
//            }
//        }

//        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter))
//
//        events = events
//                .asSequence()
//                .distinct()
//                .filterNot { it.repetitionExceptions.contains(Formatter.getDayCodeFromTS(it.startTS)) }
//                .toMutableList() as ArrayList<Event>
//
//        val eventTypeColors = LongSparseArray<Int>()
//        context.eventTypesDB.getEventTypes().forEach {
//            eventTypeColors.put(it.id!!, it.color)
//        }

//        val primaryColor = context.resources.getColor(R.color.color_primary)
//        events.forEach {
//            it.updateIsPastEvent()
//            Log.e("EventsHelper.class_forEach", "" + it.getCalDAVCalendarId() + " - " + it.getCalDAVEventId() + " - " + it.id + " - " + it.getEventStartTS() + " - " + it.startTS + " - " + it.eventType + " - " + it.importId)
//            it.color = eventTypeColors.get(it.eventType) ?: primaryColor
//        }



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
//        evetn.isPastEvent = true

        config.displayEventTypes
        events.add(eventTest)

        callback(events)
    }

//    fun getRepeatableEventsFor(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean = false): List<Event> {
//        val events = if (applyTypeFilter) {
//            val displayEventTypes = context.config.displayEventTypes
//            if (displayEventTypes.isEmpty()) {
//                return ArrayList()
//            } else {
//                eventsDB.getRepeatableEventsFromToWithTypes(toTS, context.config.getDisplayEventTypessAsList()).toMutableList() as ArrayList<Event>
//            }
//        } else {
//            if (eventId == -1L) {
//                eventsDB.getRepeatableEventsFromToWithTypes(toTS).toMutableList() as ArrayList<Event>
//            } else {
//                eventsDB.getRepeatableEventFromToWithId(eventId, toTS).toMutableList() as ArrayList<Event>
//            }
//        }
//
//        val startTimes = LongSparseArray<Long>()
//        val newEvents = ArrayList<Event>()
//        events.forEach {
//            startTimes.put(it.id!!, it.startTS)
//            if (it.repeatLimit >= 0) {
//                newEvents.addAll(getEventsRepeatingTillDateOrForever(fromTS, toTS, startTimes, it))
//            } else {
//                newEvents.addAll(getEventsRepeatingXTimes(fromTS, toTS, startTimes, it))
//            }
//        }
//
//        return newEvents
//    }
//
//    private fun getEventsRepeatingXTimes(fromTS: Long, toTS: Long, startTimes: LongSparseArray<Long>, event: Event): ArrayList<Event> {
//        val original = event.copy()
//        val events = ArrayList<Event>()
//        while (event.repeatLimit < 0 && event.startTS <= toTS) {
//            if (event.repeatInterval.isXWeeklyRepetition()) {
//                if (event.startTS.isTsOnProperDay(event)) {
//                    if (event.isOnProperWeek(startTimes)) {
//                        if (event.endTS >= fromTS) {
//                            event.copy().apply {
//                                updateIsPastEvent()
//                                color = event.color
//                                events.add(this)
//                            }
//                        }
//                        event.repeatLimit++
//                    }
//                }
//            } else {
//                if (event.endTS >= fromTS) {
//                    event.copy().apply {
//                        updateIsPastEvent()
//                        color = event.color
//                        events.add(this)
//                    }
//                } else if (event.getIsAllDay()) {
//                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
//                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
//                    if (dayCode == endDayCode) {
//                        event.copy().apply {
//                            updateIsPastEvent()
//                            color = event.color
//                            events.add(this)
//                        }
//                    }
//                }
//                event.repeatLimit++
//            }
//            event.addIntervalTime(original)
//        }
//        return events
//    }
//
//    private fun getEventsRepeatingTillDateOrForever(fromTS: Long, toTS: Long, startTimes: LongSparseArray<Long>, event: Event): ArrayList<Event> {
//        val original = event.copy()
//        val events = ArrayList<Event>()
//        while (event.startTS <= toTS && (event.repeatLimit == 0L || event.repeatLimit >= event.startTS)) {
//            if (event.endTS >= fromTS) {
//                if (event.repeatInterval.isXWeeklyRepetition()) {
//                    if (event.startTS.isTsOnProperDay(event)) {
//                        if (event.isOnProperWeek(startTimes)) {
//                            event.copy().apply {
//                                updateIsPastEvent()
//                                color = event.color
//                                events.add(this)
//                            }
//                        }
//                    }
//                } else {
//                    event.copy().apply {
//                        updateIsPastEvent()
//                        color = event.color
//                        events.add(this)
//                    }
//                }
//            }
//
//            if (event.getIsAllDay()) {
//                if (event.repeatInterval.isXWeeklyRepetition()) {
//                    if (event.endTS >= toTS && event.startTS.isTsOnProperDay(event)) {
//                        if (event.isOnProperWeek(startTimes)) {
//                            event.copy().apply {
//                                updateIsPastEvent()
//                                color = event.color
//                                events.add(this)
//                            }
//                        }
//                    }
//                } else {
//                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
//                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
//                    if (dayCode == endDayCode) {
//                        event.copy().apply {
//                            updateIsPastEvent()
//                            color = event.color
//                            events.add(this)
//                        }
//                    }
//                }
//            }
//            event.addIntervalTime(original)
//        }
//        return events
//    }
//
//    fun getRunningEvents(): List<Event> {
//        val ts = getNowSeconds()
//        val events = eventsDB.getOneTimeEventsFromTo(ts, ts).toMutableList() as ArrayList<Event>
//        events.addAll(getRepeatableEventsFor(ts, ts))
//        return events
//    }
//
//    fun getEventsToExport(includePast: Boolean, eventTypes: ArrayList<Long>): ArrayList<Event> {
//        val currTS = getNowSeconds()
//        var events = ArrayList<Event>()
//        if (includePast) {
//            events.addAll(eventsDB.getAllEventsWithTypes(eventTypes))
//        } else {
//            events.addAll(eventsDB.getOneTimeFutureEventsWithTypes(currTS, eventTypes))
//            events.addAll(eventsDB.getRepeatableFutureEventsWithTypes(currTS, eventTypes))
//        }
//
//        events = events.distinctBy { it.id } as ArrayList<Event>
//        return events
//    }
}
