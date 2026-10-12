package io.github.nayasis.simplelauncher.view

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf

/** look and feels offered in the theme menu; persisted by [name]. */
enum class AppTheme(val label: String, private val create: () -> FlatLaf) {
    INTELLIJ("IntelliJ", ::FlatIntelliJLaf),
    LIGHT("Flat Light", ::FlatLightLaf),
    MAC_LIGHT("macOS Light", ::FlatMacLightLaf),
    DARCULA("Darcula", ::FlatDarculaLaf),
    DARK("Flat Dark", ::FlatDarkLaf),
    MAC_DARK("macOS Dark", ::FlatMacDarkLaf);

    /** set as the look and feel. before any component exists, this is all a theme needs. */
    fun install() {
        FlatLaf.setup(create())
    }

    /** switch the running application to this theme: every open window is updated. */
    fun apply() {
        install()
        FlatLaf.updateUI()
    }

    companion object {
        val DEFAULT = INTELLIJ

        fun of(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
