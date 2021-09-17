package com.github.nayasis.simplelauncher.jpa.vo

import com.github.nayasis.kotlin.basica.annotation.NoArg
import com.github.nayasis.kotlin.basica.core.string.decodeBase64
import com.github.nayasis.kotlin.basica.core.string.encodeBase64
import com.github.nayasis.simplelauncher.jpa.entity.Link
import java.time.LocalDateTime

@NoArg
data class JsonLink(
    var title: String? = null,
    var group: String? = null,
    var path: String? = null,
    var relativePath: String? = null,
    var showConsole: Boolean = false,
    var option: String? = null,
    var optionPrefix: String? = null,
    var commandPrev: String? = null,
    var commandNext: String? = null,
    var description: String? = null,
    var icon: String? = null,
    var executeCount: Int = 0,
    var lastExecDate: LocalDateTime? = null,
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
        icon         = entity.icon.encodeBase64(),
        executeCount = entity.executeCount,
        lastExecDate = entity.lastExecDate,
    )

    fun toLink(): Link {
        val it = this
        return Link().apply {
            title         = it.title
            group         = it.group
            path          = it.path
            relativePath  = it.relativePath
            showConsole   = it.showConsole
            argument      = it.option
            commandPrefix = it.optionPrefix
            commandPrev   = it.commandPrev
            commandNext   = it.commandNext
            description   = it.description
            icon          = it.icon.decodeBase64()
            executeCount  = it.executeCount
            lastExecDate  = it.lastExecDate
            generateKeyword()
        }
    }

}