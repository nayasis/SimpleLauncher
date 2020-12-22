package com.github.nayasis.kotlin.basica

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String.toLocalDateTime(format: String = ""): LocalDateTime {

    val format = toDateFormat(format)
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

    return LocalDateTime.parse( value, DateTimeFormatter.ofPattern(pattern.toString()) )

}

/**
 * convert date format style from DBMS to JAVA
 * @param format
 * @return JAVA style date format
 */
private fun toDateFormat(format: String): String {
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

fun String.toLocalDateTime(format: DateTimeFormatter): LocalDateTime {
    return LocalDateTime.parse(this, format)
}

fun String.toLocalDate(format: String = ""): LocalDate {
    return this.toLocalDateTime(format).toLocalDate()
}

fun String.toLocalDate(format: DateTimeFormatter): LocalDate {
    return this.toLocalDateTime(format).toLocalDate()
}

fun LocalDate.atStartOfMonth(): LocalDate? {
    this.dayOfMonth
    return null
}

fun merong() {

    val date = "2012-01-01".toLocalDate()



}