package com.github.nayasis.simplelauncher.model.vo

import com.github.nayasis.kotlin.basica.annotation.NoArg
import com.github.nayasis.kotlin.basica.core.string.decodeBase64
import com.github.nayasis.kotlin.basica.core.string.encodeBase64
import com.github.nayasis.kotlin.javafx.misc.toBinary
import com.github.nayasis.kotlin.javafx.misc.toImage
import com.github.nayasis.simplelauncher.model.ICON_IMAGE_TYPE
import com.github.nayasis.simplelauncher.model.Link
import javafx.scene.image.Image
import java.time.LocalDateTime

@NoArg
data class JsonLink(
    var title: String?               = null,
    var group: String?               = null,
    var path: String?                = null,
    var relativePath: String?        = null,
    var showConsole: Boolean         = false,
    var option: String?              = null,
    var optionPrefix: String?        = null,
    var commandPrev: String?         = null,
    var commandNext: String?         = null,
    var description: String?         = null,
    var hashtag: String?             = null,
    var icon: String?                = null,
    var execCount: Int               = 0,
    var executedAt: LocalDateTime?   = null,
    var createdAt: LocalDateTime?    = null,
    var updatedAt: LocalDateTime?    = null,
) {

    constructor(entity: Link): this(
        title        = entity.title,
        group        = entity.group,
        path         = entity.path,
        relativePath = entity.relativePath,
        showConsole  = entity.showConsole,
        option       = entity.argument,
        optionPrefix = entity.commandPrefix,
        commandPrev  = entity.commandPrev,
        commandNext  = entity.commandNext,
        description  = entity.description,
        hashtag      = entity.hashtag,
        icon         = entity.icon?.toBinary(ICON_IMAGE_TYPE)?.encodeBase64(),
        execCount    = entity.executeCount,
        executedAt   = entity.executedAt,
        createdAt    = entity.createdAt,
        updatedAt    = entity.updatedAt,
    )

    fun toLink(): Link {
        return this.let { Link(
            title         = it.title,
            group         = it.group,
            path          = it.path,
            relativePath  = it.relativePath,
            showConsole   = it.showConsole,
            argument      = it.option,
            commandPrefix = it.optionPrefix,
            commandPrev   = it.commandPrev,
            commandNext   = it.commandNext,
            description   = it.description,
            hashtag       = it.hashtag,
            icon          = runCatching { it.icon?.decodeBase64<ByteArray>()?.toImage() }.getOrNull(),
            executeCount  = it.execCount,
            executedAt    = it.executedAt,
            createdAt     = it.createdAt ?: LocalDateTime.now(),
            updatedAt     = it.updatedAt ?: LocalDateTime.now(),
        )}
    }

}