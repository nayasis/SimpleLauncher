package io.github.nayasis.simplelauncher.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Locale

private val tokenJsonMapper = jacksonObjectMapper()
private val TOKEN_DELIMITER = "[^\\p{L}\\p{N}]+".toRegex()

fun normalizeTokens(values: Iterable<String?>?): HashSet<String> {
    return values
        ?.flatMap { it.tokenValues() }
        ?.toCollection(LinkedHashSet())
        ?: linkedSetOf()
}

fun encodeTokens(values: Iterable<String?>?): String {
    return tokenJsonMapper.writeValueAsString(normalizeTokens(values))
}

fun decodeStoredTokens(value: String?): HashSet<String> {
    if (value.isNullOrBlank()) return linkedSetOf()
    return runCatching {
        normalizeTokens(tokenJsonMapper.readValue<List<String?>>(value))
    }.getOrElse {
        linkedSetOf()
    }
}

fun parseImportedTokens(value: Any?): HashSet<String> {
    return when (value) {
        null -> linkedSetOf()
        is Iterable<*> -> normalizeTokens(value.map { it?.toString() })
        is Array<*> -> normalizeTokens(value.map { it?.toString() })
        is String -> parseImportedTokenText(value)
        else -> normalizeTokens(listOf(value.toString()))
    }
}

fun formatTokens(values: Iterable<String?>?): String {
    return normalizeTokens(values).joinToString(" ")
}

fun normalizeHashtags(values: Iterable<String?>?): HashSet<String> = normalizePlainTokens(values)

fun encodeHashtags(values: Iterable<String?>?): String {
    return tokenJsonMapper.writeValueAsString(normalizeHashtags(values))
}

fun decodeStoredHashtags(value: String?): HashSet<String> {
    if (value.isNullOrBlank()) return linkedSetOf()
    return runCatching {
        normalizeHashtags(tokenJsonMapper.readValue<List<String?>>(value))
    }.getOrElse {
        linkedSetOf()
    }
}

fun parseImportedHashtags(value: Any?): HashSet<String> {
    return when (value) {
        null -> linkedSetOf()
        is Iterable<*> -> normalizeHashtags(value.map { it?.toString() })
        is Array<*> -> normalizeHashtags(value.map { it?.toString() })
        is String -> parseImportedHashtagText(value)
        else -> normalizeHashtags(listOf(value.toString()))
    }
}

private fun parseImportedTokenText(value: String): HashSet<String> {
    val text = value.trim()
    if (text.isEmpty()) return linkedSetOf()
    if (text.startsWith("[")) {
        decodeStoredTokens(text).takeIf { it.isNotEmpty() }?.let { return it }
    }
    return normalizeTokens(listOf(text))
}

private fun parseImportedHashtagText(value: String): HashSet<String> {
    val text = value.trim()
    if (text.isEmpty()) return linkedSetOf()
    if (text.startsWith("[")) {
        decodeStoredHashtags(text).takeIf { it.isNotEmpty() }?.let { return it }
    }
    return normalizePlainTokens(text.split("[,\\s]+".toRegex()))
}

private fun String?.tokenValues(): List<String> {
    return this
        ?.trim()
        ?.split(TOKEN_DELIMITER)
        ?.mapNotNull { it.trim().lowercase(Locale.ROOT).takeIf(String::isNotEmpty) }
        ?: emptyList()
}

private fun normalizePlainTokens(values: Iterable<String?>?): HashSet<String> {
    return values
        ?.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        ?.toCollection(LinkedHashSet())
        ?: linkedSetOf()
}
