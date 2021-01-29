package com.github.nayasis.kotlin.basica

import com.github.nayasis.basica.model.Messages
import mu.KotlinLogging
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

private val log = KotlinLogging.logger {}

fun String.message(locale: Locale? = null): String = Messages.get(locale, this)

fun String.toPath(): Path = Paths.get(this.trim())

fun String.toDir(filecheck: Boolean = true): Path? {
    val path = this.toPath()
    return when {
        ! filecheck -> path.parent
        path.isDirectory() -> path
        else -> path.parent
    }
}

fun String.toFile(): File = File(this)

fun String.toUrl(raiseException: Boolean = false): URL? {
    return try {
        URL(this)
    } catch ( e : MalformedURLException ) {
        log.trace(e.message, e)
        if( raiseException ) throw e
        return null
    }
}

fun String.decodeBase64(): ByteArray = Base64.getDecoder().decode(this)

fun ByteArray.encodeBase64(): String = Base64.getMimeEncoder().encodeToString(this)

fun String.found(pattern: Pattern?): Boolean {
    return pattern?.matcher(this)?.find() ?: false
}

fun String.found(pattern: Regex): Boolean {
    return found( pattern.toPattern() )
}