package com.example.simplecalendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.simplecalendar.extensions.recheckCalDAVCalendars
import com.example.simplecalendar.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.recheckCalDAVCalendars {
            context.updateWidgets()
        }
    }
}
