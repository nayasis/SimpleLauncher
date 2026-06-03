package io.github.nayasis.simplelauncher.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

private val tokenJsonMapper = jacksonObjectMapper()
private val IMPORT_TOKEN_DELIMITER = "[,\\s]+".toRegex()

fun normalizeTokens(values: Iterable<String?>?): HashSet<String> {
    return values
        ?.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
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

fun normalizeHashtags(values: Iterable<String?>?): HashSet<String> = normalizeTokens(values)

fun encodeHashtags(values: Iterable<String?>?): String = encodeTokens(values)

fun decodeStoredHashtags(value: String?): HashSet<String> = decodeStoredTokens(value)

fun parseImportedHashtags(value: Any?): HashSet<String> = parseImportedTokens(value)

private fun parseImportedTokenText(value: String): HashSet<String> {
    val text = value.trim()
    if (text.isEmpty()) return linkedSetOf()
    if (text.startsWith("[")) {
        decodeStoredTokens(text).takeIf { it.isNotEmpty() }?.let { return it }
    }
    return normalizeTokens(text.split(IMPORT_TOKEN_DELIMITER))
}
