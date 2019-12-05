package com.example.simplecalendar.extensions

import android.accounts.Account
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.AlarmManagerCompat
import com.example.simplecalendar.R
import com.example.simplecalendar.activities.EventActivity
import com.example.simplecalendar.databases.EventsDatabase
import com.example.simplecalendar.helpers.*
import com.example.simplecalendar.helpers.Formatter
import com.example.simplecalendar.interfaces.EventTypesDao
import com.example.simplecalendar.interfaces.EventsDao
import com.example.simplecalendar.models.*
import com.example.simplecalendar.receivers.CalDAVSyncReceiver
import com.example.simplecalendar.receivers.NotificationReceiver
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.eventsDB: EventsDao get() = EventsDatabase.getInstance(applicationContext).EventsDao()
val Context.eventTypesDB: EventTypesDao get() = EventsDatabase.getInstance(applicationContext).EventTypesDao()
val Context.eventsHelper: EventsHelper get() = EventsHelper(this)
val Context.calDAVHelper: CalDAVHelper get() = CalDAVHelper(this)

fun Context.updateWidgets() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(ComponentName(applicationContext, MyWidgetMonthlyProvider::class.java))
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetMonthlyProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }

    updateListWidget()
}

fun Context.updateListWidget() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(ComponentName(applicationContext, MyWidgetListProvider::class.java))
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetListProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
}

fun Context.getRepetitionText(seconds: Int) = when (seconds) {
    0 -> getString(R.string.no_repetition)
    DAY -> getString(R.string.daily)
    WEEK -> getString(R.string.weekly)
    MONTH -> getString(R.string.monthly)
    YEAR -> getString(R.string.yearly)
    else -> {
        when {
            seconds % YEAR == 0 -> resources.getQuantityString(R.plurals.years, seconds / YEAR, seconds / YEAR)
            seconds % MONTH == 0 -> resources.getQuantityString(R.plurals.months, seconds / MONTH, seconds / MONTH)
            seconds % WEEK == 0 -> resources.getQuantityString(R.plurals.weeks, seconds / WEEK, seconds / WEEK)
            else -> resources.getQuantityString(R.plurals.days, seconds / DAY, seconds / DAY)
        }
    }
}

fun Context.launchNewEventIntent(dayCode: String = Formatter.getTodayCode()) {
    Intent(applicationContext, EventActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

fun Context.getNewEventTimestampFromCode(dayCode: String): Long {
    val defaultStartTime = config.defaultStartTime
    val currHour = DateTime(System.currentTimeMillis(), DateTimeZone.getDefault()).hourOfDay
    var dateTime = Formatter.getLocalDateTimeFromCode(dayCode).withHourOfDay(currHour)
    var newDateTime = dateTime.plusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)

    if (defaultStartTime != -1) {
        val hours = defaultStartTime / 60
        val minutes = defaultStartTime % 60
        dateTime = Formatter.getLocalDateTimeFromCode(dayCode).withHourOfDay(hours).withMinuteOfHour(minutes)
        newDateTime = dateTime
    }

    // make sure the date doesn't change
    return newDateTime.withDate(dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth).seconds()
}

fun Context.recheckCalDAVCalendars(callback: () -> Unit) {
    if (config.caldavSync) {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(false, callback)
            updateWidgets()
        }
    }
}

fun Context.scheduleCalDAVSync(activate: Boolean) {
    val syncIntent = Intent(applicationContext, CalDAVSyncReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (activate) {
        val syncCheckInterval = 2 * AlarmManager.INTERVAL_HOUR
        try {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + syncCheckInterval, syncCheckInterval, pendingIntent)
        } catch (ignored: Exception) {
        }
    } else {
        alarm.cancel(pendingIntent)
    }
}

fun Context.addDayNumber(rawTextColor: Int, day: DayMonthly, linearLayout: LinearLayout, dayLabelHeight: Int, callback: (Int) -> Unit) {
    var textColor = rawTextColor
    if (!day.isThisMonth)
        textColor = textColor.adjustAlpha(LOW_ALPHA)

    (View.inflate(applicationContext, R.layout.day_monthly_number_view, null) as TextView).apply {
        setTextColor(textColor)
        text = day.value.toString()
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        linearLayout.addView(this)

        if (day.isToday) {
            val primaryColor = getAdjustedPrimaryColor()
            setTextColor(primaryColor.getContrastColor())
            if (dayLabelHeight == 0) {
                onGlobalLayout {
                    val height = this@apply.height
                    if (height > 0) {
                        callback(height)
                        addTodaysBackground(this, resources, height, primaryColor)
                    }
                }
            } else {
                addTodaysBackground(this, resources, dayLabelHeight, primaryColor)
            }
        }
    }
}

private fun addTodaysBackground(textView: TextView, res: Resources, dayLabelHeight: Int, primaryColor: Int) =
        textView.addResizedBackgroundDrawable(res, dayLabelHeight, primaryColor, R.drawable.ic_circle_filled)

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun Context.addDayEvents(day: DayMonthly, linearLayout: LinearLayout, res: Resources, dividerMargin: Int) {
    val eventLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    day.dayEvents.sortedWith(compareBy<Event> {
        if (it.getIsAllDay()) {
            Formatter.getDayStartTS(Formatter.getDayCodeFromTS(it.startTS)) - 1
        } else {
            it.startTS
        }
    }.thenBy {
        if (it.getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(it.endTS))
        } else {
            it.endTS
        }
    }.thenBy { it.title }).forEach {
        val backgroundDrawable = res.getDrawable(R.drawable.day_monthly_event_background)
        backgroundDrawable.applyColorFilter(it.color)
        eventLayoutParams.setMargins(dividerMargin, 0, dividerMargin, dividerMargin)

        var textColor = it.color.getContrastColor()
        if (!day.isThisMonth) {
            backgroundDrawable.alpha = 64
            textColor = textColor.adjustAlpha(0.25f)
        }

        (View.inflate(applicationContext, R.layout.day_monthly_event_view, null) as TextView).apply {
            setTextColor(textColor)
            text = it.title.replace(" ", "\u00A0")  // allow word break by char
            background = backgroundDrawable
            layoutParams = eventLayoutParams
            contentDescription = it.title
            linearLayout.addView(this)
        }
    }
}

fun Context.getEventListItems(events: List<Event>): ArrayList<ListItem> {
    val listItems = ArrayList<ListItem>(events.size)
    val replaceDescription = config.replaceDescription

    // move all-day events in front of others
    val sorted = events.sortedWith(compareBy<Event> {
        if (it.getIsAllDay()) {
            Formatter.getDayStartTS(Formatter.getDayCodeFromTS(it.startTS)) - 1
        } else {
            it.startTS
        }
    }.thenBy {
        if (it.getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(it.endTS))
        } else {
            it.endTS
        }
    }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description })

    var prevCode = ""
    val now = getNowSeconds()
    val today = Formatter.getDayTitle(this, Formatter.getDayCodeFromTS(now))

    sorted.forEach {
        val code = Formatter.getDayCodeFromTS(it.startTS)
        if (code != prevCode) {
            val day = Formatter.getDayTitle(this, code)
            val isToday = day == today
            val listSection = ListSection(day, code, isToday, !isToday && it.startTS < now)
            listItems.add(listSection)
            prevCode = code
        }
        val listEvent = ListEvent(it.id!!, it.startTS, it.endTS, it.title, it.description, it.getIsAllDay(), it.color, it.location, it.isPastEvent, it.repeatInterval > 0)
        listItems.add(listEvent)
    }
    return listItems
}

fun Context.handleEventDeleting(eventIds: List<Long>, timestamps: List<Long>, action: Int) {
    when (action) {
        DELETE_SELECTED_OCCURRENCE -> {
            eventIds.forEachIndexed { index, value ->
                eventsHelper.addEventRepetitionException(value, timestamps[index], true)
            }
        }
        DELETE_FUTURE_OCCURRENCES -> {
            eventIds.forEachIndexed { index, value ->
                eventsHelper.addEventRepeatLimit(value, timestamps[index])
            }
        }
        DELETE_ALL_OCCURRENCES -> {
            eventsHelper.deleteEvents(eventIds.toMutableList(), true)
        }
    }
}

fun Context.refreshCalDAVCalendars(ids: String, showToasts: Boolean) {
    val uri = CalendarContract.Calendars.CONTENT_URI
    val accounts = HashSet<Account>()
    val calendars = calDAVHelper.getCalDAVCalendars(ids, showToasts)
    calendars.forEach {
        accounts.add(Account(it.accountName, it.accountType))
    }

    Bundle().apply {
        putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        accounts.forEach {
            ContentResolver.requestSync(it, uri.authority, this)
        }
    }
}
