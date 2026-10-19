package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.klass.Classes
import io.github.nayasis.kotlin.basica.model.Messages
import java.util.Locale

private const val BUNDLE_PATTERN = "/message/localization.*.prop"

/**
 * languages offered in the language menu, one per message bundle.
 *
 * adding `localization.<lang>[_<COUNTRY>].prop` is all a new language needs; its `language.name` is the menu label.
 */
object Languages {

    /** most spoken first; bundles not listed here follow in tag order. */
    private val ORDER = listOf("en", "zh-CN", "es", "fr", "ja", "ko", "zh-TW")

    val available: List<Locale> by lazy {
        Classes.findResources(BUNDLE_PATTERN)
            .mapNotNull { localeOf(it.path.substringAfterLast('/')) }
            .distinct()
            .sortedWith(compareBy({ rank(it) }, { it.toLanguageTag() }))
    }

    /** the bundle locale the messages are shown in, resolved as [Messages] does: exact match, then language only, then English. */
    fun current(): Locale {
        val default = Messages.defaultLocale
        return available.firstOrNull { it == default }
            ?: available.firstOrNull { it.language == default.language }
            ?: Locale.ENGLISH
    }

    /** show messages in [locale] from now on. Messages keeps the default it saw on first use, so set both. */
    fun apply(locale: Locale) {
        Locale.setDefault(locale)
        Messages.defaultLocale = locale
    }

    fun label(locale: Locale): String =
        Messages["language.name", locale].ifEmpty { locale.getDisplayName(locale) }

    /** the locale a bundle file is for, read the way [Messages] reads it: "zh_CN" is language "zh", country "CN". */
    private fun localeOf(fileName: String): Locale? {
        val tag = fileName.removeSuffix(".prop").split('.').takeIf { it.size > 1 }?.last() ?: return null
        return Locale.of(tag.filter { it.isLowerCase() }, tag.filter { it.isUpperCase() })
    }

    private fun rank(locale: Locale): Int =
        ORDER.indexOf(locale.toLanguageTag()).takeIf { it >= 0 } ?: ORDER.size

}
