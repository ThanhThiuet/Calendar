package com.example.simplecalendar.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.example.simplecalendar.adapters.EventListWidgetAdapterEmpty

class WidgetServiceEmpty : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapterEmpty(applicationContext)
}
