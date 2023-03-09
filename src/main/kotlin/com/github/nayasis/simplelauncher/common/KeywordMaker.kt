package com.github.nayasis.simplelauncher.common

import com.github.nayasis.kotlin.basica.core.character.toHalfWidth
import java.lang.Character.*

fun makeKeywords(text: String): Set<String> {

    val words  = mutableSetOf<String>()
    val string = removeUnusedWord(text.lowercase()).map { Ch(it.toHalfWidth()) }
    val sb     = StringBuilder()

    string.forEachIndexed { i, curr ->
        if(curr.isNone) {
            sb.flush(words)
        } else {
            sb.append(curr.c)
        }
    }
    sb.flush(words)

    return words

}

private fun removeUnusedWord(text: String): String {
    var rs = text
    REGEXES_REMOVABLE.forEach { rs = rs.replace(it, " ") }
    return rs
}

private val REGEXES_REMOVABLE = listOf(
    "(?i), the( |$)".toRegex(),
    "(?i)^the ".toRegex(),
    "(?i)'s( |$) ".toRegex(),
)

private fun StringBuilder.flush(set: MutableSet<String>) {
    if(this.isNotEmpty()) {
        set.add(this.toString().lowercase())
        this.clear()
    }
}

private fun isDigit(c: Char): Boolean {
    return c.code.let { code ->
        digits.any { code.between(it) }
    }
}

private fun isChar(c: Char): Boolean {
    return if (UnicodeBlock.of(c) in charBlocks) true else
        c.code.let { code ->
            characters.any { code.between(it) }
        }
}

private data class Ch (
    val c: Char,
    val isDigit: Boolean = isDigit(c),
    val isChar: Boolean = isChar(c),
) {
    val isNone: Boolean
        get() = ! isDigit && ! isChar
}

private val charBlocks = setOf(
    UnicodeBlock.LATIN_EXTENDED_A,
    UnicodeBlock.LATIN_EXTENDED_B,
    UnicodeBlock.LATIN_EXTENDED_ADDITIONAL,
    UnicodeBlock.IPA_EXTENSIONS,
    UnicodeBlock.GREEK,
    UnicodeBlock.GREEK_EXTENDED,
    UnicodeBlock.CYRILLIC,
    UnicodeBlock.CYRILLIC_SUPPLEMENTARY,
    UnicodeBlock.CYRILLIC_EXTENDED_A,
    UnicodeBlock.CYRILLIC_EXTENDED_B,
    UnicodeBlock.CYRILLIC_EXTENDED_C,
    UnicodeBlock.ARMENIAN,
    UnicodeBlock.ARABIC,
    UnicodeBlock.HEBREW,
    UnicodeBlock.SYRIAC,
    UnicodeBlock.SYRIAC_SUPPLEMENT,
    UnicodeBlock.MANDAIC,
    UnicodeBlock.SAMARITAN,
    UnicodeBlock.THAANA,
    UnicodeBlock.DEVANAGARI,
    UnicodeBlock.DEVANAGARI_EXTENDED,
    UnicodeBlock.BENGALI,
    UnicodeBlock.GURMUKHI,
    UnicodeBlock.GUJARATI,
    UnicodeBlock.ORIYA,
    UnicodeBlock.TAMIL,
    UnicodeBlock.TELUGU,
    UnicodeBlock.KANNADA,
    UnicodeBlock.MALAYALAM,
    UnicodeBlock.SINHALA,
    UnicodeBlock.SINHALA_ARCHAIC_NUMBERS,
    UnicodeBlock.GEORGIAN,
    UnicodeBlock.ETHIOPIC,
    UnicodeBlock.MONGOLIAN,
    UnicodeBlock.HIRAGANA,
    UnicodeBlock.KATAKANA,
    UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
    UnicodeBlock.BOPOMOFO,
    UnicodeBlock.BOPOMOFO_EXTENDED,
    UnicodeBlock.HANGUL_JAMO,
    UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
    UnicodeBlock.HANGUL_SYLLABLES,
    UnicodeBlock.HANGUL_JAMO_EXTENDED_A,
    UnicodeBlock.HANGUL_JAMO_EXTENDED_B,
    UnicodeBlock.KANBUN,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E,
    UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F,
    UnicodeBlock.SHAVIAN,
)

private val digits = listOf(
    Pair("0030".toInt(16),"0039".toInt(16)), // 0 ~ 9
)

private val characters = listOf(
    Pair("0041".toInt(16),"005A".toInt(16)), // A ~ Z
    Pair("0061".toInt(16),"007A".toInt(16)), // a ~ z
    Pair("00C0".toInt(16),"00D6".toInt(16)), // À ~ Ö
    Pair("00D8".toInt(16),"00DE".toInt(16)), // Ø ~ Þ
    Pair("00DF".toInt(16),"00F6".toInt(16)), // ß ~ ö
    Pair("00F8".toInt(16),"00FF".toInt(16)), // ø ~ ÿ
    Pair("0100".toInt(16),"017F".toInt(16)), // Ā ~ ſ
    Pair("3063".toInt(16),"30FC".toInt(16)), // っ,ッ, ー
    Pair("2139".toInt(16),"2139".toInt(16)), // 々
    Pair("4EDD".toInt(16),"4EDD".toInt(16)), // 仝
    Pair("30FD".toInt(16),"30FE".toInt(16)), // ヽ, ヾ
    Pair("309D".toInt(16),"309E".toInt(16)), // ゝ, ゞ
    Pair("309D".toInt(16),"309E".toInt(16)), // ゝ, ゞ
    Pair("3003".toInt(16),"3003".toInt(16)), // 〃
    Pair("3031".toInt(16),"3035".toInt(16)), // 〱 ~ 〵
)

private fun Int.between(pair: Pair<Int,Int>): Boolean {
    return pair.first <= this && this <= pair.second
}
