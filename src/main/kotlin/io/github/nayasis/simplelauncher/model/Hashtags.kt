package io.github.nayasis.simplelauncher.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

private val hashtagJsonMapper = jacksonObjectMapper()
private val IMPORT_TOKEN_DELIMITER = "[,\\s]+".toRegex()

fun normalizeHashtags(values: Iterable<String?>?): HashSet<String> {
    return values
        ?.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        ?.toCollection(LinkedHashSet())
        ?: linkedSetOf()
}

fun encodeHashtags(values: Iterable<String?>?): String {
    return hashtagJsonMapper.writeValueAsString(normalizeHashtags(values))
}

fun decodeStoredHashtags(value: String?): HashSet<String> {
    if (value.isNullOrBlank()) return linkedSetOf()
    return runCatching {
        normalizeHashtags(hashtagJsonMapper.readValue<List<String?>>(value))
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

private fun parseImportedHashtagText(value: String): HashSet<String> {
    val text = value.trim()
    if (text.isEmpty()) return linkedSetOf()
    if (text.startsWith("[")) {
        decodeStoredHashtags(text).takeIf { it.isNotEmpty() }?.let { return it }
    }
    return normalizeHashtags(text.split(IMPORT_TOKEN_DELIMITER))
}
