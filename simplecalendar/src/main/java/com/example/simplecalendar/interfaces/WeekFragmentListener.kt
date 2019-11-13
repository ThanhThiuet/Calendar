package com.example.simplecalendar.interfaces

interface WeekFragmentListener {
    fun scrollTo(y: Int)

    fun updateHoursTopMargin(margin: Int)

    fun getCurrScrollY(): Int
}
