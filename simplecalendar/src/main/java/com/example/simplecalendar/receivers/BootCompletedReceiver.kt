package com.example.simplecalendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.simplecalendar.extensions.notifyRunningEvents
import com.example.simplecalendar.extensions.recheckCalDAVCalendars
import com.example.simplecalendar.extensions.scheduleAllEvents
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                scheduleAllEvents()
                notifyRunningEvents()
                recheckCalDAVCalendars {}
            }
        }
    }
}
