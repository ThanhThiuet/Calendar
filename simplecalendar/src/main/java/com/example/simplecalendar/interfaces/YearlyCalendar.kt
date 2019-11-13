package com.example.simplecalendar.interfaces

import android.util.SparseArray
import com.example.simplecalendar.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
