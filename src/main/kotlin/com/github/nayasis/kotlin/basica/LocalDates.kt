package com.github.nayasis.kotlin.basica

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

/**
 * convert string to LocalDateTime
 *
 * @receiver String
 * @param format DateTimeFormatter
 * @return LocalDateTime
 */
@Suppress("NAME_SHADOWING")
fun String.toLocalDateTime(format: String = ""): LocalDateTime {

    val format = parseFormat(format)
    val digits = this.replace("[^0-9\\+]".toRegex(), "")

    val size = kotlin.math.min( format.length, digits.replace("+","").length )

    val pattern = StringBuilder()
    val value   = StringBuilder()

    var k = 0
    for( i in 0 until size) {
        val c = format[i]
        pattern.append(c)
        if( c == 'Z' ) {
            repeat(5) { value.append(digits[k++]) }
        } else {
            value.append( digits[k++] )
        }
    }

    val ofPattern = DateTimeFormatter.ofPattern(pattern.toString())

    return try {
        LocalDateTime.parse( value, ofPattern)
    } catch (e: Exception) {
        try {
            LocalDate.parse(value, ofPattern).atTime(0,0)
        } catch (_: Exception) {
            throw e
        }
    }

}

private fun parseFormat(format: String): String {
    if( format.isEmpty() ) return "yyyyMMddHHmmssSSS"
    return format
        .replace("'.*?'".toRegex(), " ") // remove user text
        .replace("YYYY".toRegex(), "yyyy")
        .replace("(^|[^D])DD([^D]|$)".toRegex(), "$1dd$2")
        .replace("MI".toRegex(), "mm")
        .replace("(^|[^S])SS([^S]|$)".toRegex(), "$1ss$2")
        .replace("(^|[^F])FFF([^F]|$)".toRegex(), "$1SSS$2")
        .replace("[^yMdHmsSZ]".toRegex(), "")
}

private fun printFormat(format: String, default: DateTimeFormatter): DateTimeFormatter {
    if( format.isEmpty() ) return default
    return DateTimeFormatter.ofPattern( format
        .replace("YYYY".toRegex(), "yyyy")
        .replace("(^|[^D])DD([^D]|$)".toRegex(), "$1dd$2")
        .replace("MI".toRegex(), "mm")
        .replace("(^|[^S])SS([^S]|$)".toRegex(), "$1ss$2")
        .replace("(^|[^F])FFF([^F]|$)".toRegex(), "$1SSS$2")
    )
}

fun String.toLocalDateTime(format: DateTimeFormatter): LocalDateTime {
    return LocalDateTime.parse(this, format)
}

fun String.toLocalDate(format: String = ""): LocalDate {
    return this.toLocalDateTime(format).toLocalDate()
}

fun String.toLocalDate(format: DateTimeFormatter): LocalDate {
    return this.toLocalDateTime(format).toLocalDate()
}

fun LocalDateTime.atStartOfMonth(): LocalDateTime {
    return this.withDayOfMonth(1)
}

fun LocalDateTime.atEndOfMonth(): LocalDateTime {
    return this.withDayOfMonth(this.toLocalDate().lengthOfMonth())
}

fun LocalDate.atStartOfMonth(): LocalDate {
    return this.withDayOfMonth(1)
}

fun LocalDate.atEndOfMonth(): LocalDate {
    return this.withDayOfMonth(this.lengthOfMonth())
}

fun LocalDateTime.toString(format: String = ""): String {
    return this.format( printFormat(format, ISO_LOCAL_DATE_TIME) )
}

fun LocalDate.toString(format: String = ""): String {
    return this.format( printFormat(format, ISO_LOCAL_DATE) )
}