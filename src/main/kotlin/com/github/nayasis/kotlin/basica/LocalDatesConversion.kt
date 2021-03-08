package com.github.nayasis.kotlin.basica

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.sql.Date as SqlDate

// [LocalDateTime -> LocalDate] is already exists.

fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from( this.toZonedDateTime(zoneId).toInstant() )
}

fun LocalDateTime.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return this.atZone(zoneId)
}

fun LocalDateTime.toSqlDate(): SqlDate {
    return SqlDate.valueOf(this.toLocalDate())
}

fun LocalDate.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
    return Date.from( this.atStartOfDay(zoneId).toInstant() )
}

fun LocalDate.toLocalDateTime(): LocalDateTime {
    return this.atStartOfDay()
}

fun LocalDate.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return this.atStartOfDay(zoneId)
}

fun LocalDate.toSqlDate(): SqlDate {
    return SqlDate.valueOf(this)
}

fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return this.toZonedDateTime(zoneId).toLocalDateTime()
}

fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    return this.toZonedDateTime(zoneId).toLocalDate()
}

fun Date.toZonedDateTime(zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
    return ZonedDateTime.ofInstant(this.toInstant(),zoneId)
}

fun Date.toSqlDate(zoneId: ZoneId = ZoneId.systemDefault()): SqlDate {
    return SqlDate.valueOf(this.toLocalDate(zoneId))
}