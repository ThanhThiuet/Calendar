package com.example.simplecalendar.services

import android.app.IntentService
import android.content.Intent
import com.example.simplecalendar.extensions.config
import com.example.simplecalendar.extensions.eventsDB
import com.example.simplecalendar.extensions.rescheduleReminder
import com.example.simplecalendar.helpers.EVENT_ID

abstract class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        val eventId = intent?.getLongExtra(EVENT_ID, 0L)
        val event = eventId?.let { eventsDB.getEventWithId(it) }
        rescheduleReminder(event, config.snoozeTime)
    }
}
