package com.example.biblelog.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

fun currentInstant(): Instant = Clock.System.now()

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
