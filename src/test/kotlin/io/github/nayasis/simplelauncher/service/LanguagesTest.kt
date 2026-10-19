package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.model.Messages
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale

internal class LanguagesTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadMessages() {
            "/message/localization.*.prop".loadMessages()
        }
    }

    @Test
    fun everyBundleIsOfferedMostSpokenFirst() {
        Languages.available.map { it.toLanguageTag() } shouldBe listOf("en", "zh-CN", "es", "fr", "ja", "ko", "zh-TW")
        Languages.available.map { Languages.label(it) } shouldBe
            listOf("English", "简体中文", "Español", "Français", "日本語", "한국어", "繁體中文")
    }

    @Test
    fun aSavedTagFindsItsBundle() {
        Languages.available.forEach { locale ->
            Messages["menu.settings.language", Locale.forLanguageTag(locale.toLanguageTag())] shouldBe
                Messages["menu.settings.language", locale]
        }
        Messages["menu.settings.language", Locale.forLanguageTag("zh-TW")] shouldBe "語言"
    }

    @Test
    fun currentFallsBackToTheLanguageThenEnglish() {
        val saved = Messages.defaultLocale
        try {
            Messages.defaultLocale = Locale.forLanguageTag("ko-KR")
            Languages.current() shouldBe Locale.of("ko")
            Messages["menu.settings.language"] shouldBe "언어"
            Messages.defaultLocale = Locale.forLanguageTag("zh-TW")
            Languages.current() shouldBe Locale.of("zh", "TW")
            Messages["menu.settings.language"] shouldBe "語言"
            // no bundle for German: English, the same as the messages fall back to
            Messages.defaultLocale = Locale.forLanguageTag("de-DE")
            Languages.current() shouldBe Locale.ENGLISH
            Messages["menu.settings.language"] shouldBe "Language"
        } finally {
            Messages.defaultLocale = saved
        }
    }

}
