package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.extention.ifEmpty
import com.github.nayasis.kotlin.basica.core.path.*
import com.github.nayasis.kotlin.basica.core.string.format.DEFAULT_BINDER
import com.github.nayasis.kotlin.basica.core.string.format.ExtractPattern
import com.github.nayasis.kotlin.basica.core.string.format.Formatter
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.toPath
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.simplelauncher.common.Context
import com.github.nayasis.simplelauncher.common.wrapDoubleQuote
import com.github.nayasis.simplelauncher.jpa.entity.Link
import java.io.File
import java.nio.file.Path

private val PATTERN_KEYWORD = ExtractPattern("\\$\\{([^\\s{}].*?)}".toPattern())

@Suppress("MemberVisibilityCanBePrivate", "JoinDeclarationAndAssignment")
class LinkCommand {

    var title: String?

    var path: Path?
    var workingDirectory: String?

    var argument: String
    var commandPrefix: String
    var commandPrev: String
    var commandNext: String
    var showConsole: Boolean

    constructor(link: Link) {
        title            = link.title
        path             = getExecutionPath(link)
        workingDirectory = path?.directory?.pathString
        argument         = link.argument ?: ""
        commandPrefix    = link.commandPrefix ?: ""
        commandPrev      = link.commandPrev ?: ""
        commandNext      = link.commandNext ?: ""
        showConsole      = link.showConsole
    }

    constructor(link: Link, file: File?): this(link) {
        if( file != null ) bindOption(file)
    }

    constructor(link: Link, files: Collection<File>?): this(link) {
        if( ! files.isNullOrEmpty() ) bindOption(files)
    }

    fun bindOption(file: File): LinkCommand {
        val prevArgument = argument
        bind(file)
        if( prevArgument == argument ) {
            argument += " " + file.path.wrapDoubleQuote()
        }
        return this
    }

    fun bindOption(files: Collection<File>) {
        val prevArgument = argument
        bind(files.first())
        if( prevArgument == argument ) {
            @Suppress("SimplifiableCallChain")
            argument += " " + files.map { it.path.wrapDoubleQuote() }.joinToString(" ")
        }
    }

    private fun bind(file: File) {
        toParameter(file).let {
            argument      = bindOption(argument, it)
            commandPrefix = bindOption(commandPrefix, it)
            commandPrev   = bindOption(commandPrev, it)
            commandNext   = bindOption(commandNext, it)
        }
    }

    private fun toParameter(file: File?): Map<String,String> {
        return HashMap<String,String>().apply {
            if( file == null || ! file.exists() ) return this
            this["path"]      = file.absolutePath
            this["path-unix"]      = file.invariantSeparatorsPath
            this["dir"]       = file.directory.absolutePath
            this["dir-unix"]  = file.directory.invariantSeparatorsPath
            this["file"]      = file.name
            this["name"]      = file.nameWithoutExtension
            this["ext"]       = file.extension
            this["home"]      = userHome().pathString
            this["home-unix"] = userHome().invariantSeparators
        }
    }

    private fun bindOption(option: String?, param: Map<String,String>): String {
        if( option.isNullOrEmpty() ) return ""
        return Formatter().bind(PATTERN_KEYWORD, option, DEFAULT_BINDER, false, param)
    }

    private fun getExecutionPath(link: Link): Path? {

        if( link.path == null ) return null

        var path = link.path!!.toPath()
        if( path.exists() ) return path

        path = rootPath() / link.path.ifEmpty{""}
        if( path.exists() ) return path

        path = rootPath() / link.relativePath.ifEmpty{""}
        if( path.exists() ) {
            link.path = path.pathString
            Context.linkService.save(link)
            return path
        }

        return link.path!!.toPath()

    }

    fun toCommand(): Command {
        return Command(workingDirectory=workingDirectory).appendParsing(commandPrefix).append(path?.pathString)
            .also { if(it.isEmpty()) throw IllegalArgumentException("msg.err.007".message().format(title)) }
            .append(argument)


    }

}