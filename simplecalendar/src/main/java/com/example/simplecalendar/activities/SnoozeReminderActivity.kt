package com.example.simplecalendar.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.simplecalendar.extensions.config
import com.example.simplecalendar.extensions.eventsDB
import com.example.simplecalendar.extensions.rescheduleReminder
import com.example.simplecalendar.helpers.EVENT_ID
import com.simplemobiletools.commons.extensions.showPickSecondsDialogHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showPickSecondsDialogHelper(config.snoozeTime, true, cancelCallback = { dialogCancelled() }) {
            ensureBackgroundThread {
                val eventId = intent.getLongExtra(EVENT_ID, 0L)
                val event = eventsDB.getEventWithId(eventId)
                config.snoozeTime = it / 60
                rescheduleReminder(event, it / 60)
                runOnUiThread {
                    finishActivity()
                }
            }
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
