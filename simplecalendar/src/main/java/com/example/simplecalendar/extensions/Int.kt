package com.example.simplecalendar.extensions

import com.example.simplecalendar.helpers.MONTH
import com.example.simplecalendar.helpers.WEEK
import com.example.simplecalendar.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0
