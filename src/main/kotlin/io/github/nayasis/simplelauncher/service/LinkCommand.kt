package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.directory
import io.github.nayasis.kotlin.basica.core.io.invariantPath
import io.github.nayasis.kotlin.basica.core.io.pathString
import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.tokenize
import io.github.nayasis.kotlin.basica.etc.Platforms
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.simplelauncher.common.wrapDoubleQuote
import io.github.nayasis.simplelauncher.model.Link
import java.io.File

private val PATTERN_VARIABLE = "\\$\\{([^\\s{}].*?)}".toRegex()

/** variables taken from a dropped file. */
private val FILE_VARIABLES = setOf("path", "path-unix", "dir", "dir-unix", "file", "name", "ext")

@Suppress("MemberVisibilityCanBePrivate")
class LinkCommand {

    var title: String?

    var path: String?
    var workingDirectory: String?

    var argument: String
    var commandPrefix: String
    var commandPrev: String
    var commandNext: String
    var showConsole: Boolean

    constructor(link: Link) {
        // resolve first: finding the file by its relative path corrects link.path
        val resolved     = link.toPath()
        val home         = homeParameters()
        title            = link.title
        argument         = bind(link.argument ?: "", home)
        commandPrefix    = bind(link.commandPrefix ?: "", home)
        commandPrev      = bind(link.commandPrev ?: "", home)
        commandNext      = bind(link.commandNext ?: "", home)
        showConsole      = link.showConsole
        path             = resolved?.pathString ?: link.path
        workingDirectory = resolved?.directory?.pathString
    }

    constructor(link: Link, file: File?): this(link, listOfNotNull(file))

    constructor(link: Link, files: Collection<File>?): this(link) {
        if( ! files.isNullOrEmpty() ) bindFiles(files)
    }

    /**
     * pass [files] to the command.
     *
     * when no field uses a file variable, the files are appended to the argument. otherwise the argument
     * repeats once per file, and so does every line of the previous / next commands that uses one;
     * the prefix takes the first file.
     */
    private fun bindFiles(files: Collection<File>) {
        if( listOf(argument, commandPrefix, commandPrev, commandNext).none { it.hasFileVariable() } ) {
            argument = (listOf(argument) + files.map { it.path.wrapDoubleQuote() })
                .filter { it.isNotBlank() }
                .joinToString(" ")
            return
        }
        val params = files.map { fileParameters(it) }
        if( argument.hasFileVariable() ) {
            argument = params.joinToString(" ") { bind(argument, it) }
        }
        commandPrefix = bind(commandPrefix, params.first())
        commandPrev   = bindLines(commandPrev, params)
        commandNext   = bindLines(commandNext, params)
    }

    private fun bindLines(script: String, params: List<Map<String,String>>): String {
        return script.lines().flatMap { line ->
            if( line.hasFileVariable() ) params.map { bind(line, it) } else listOf(line)
        }.joinToString("\n")
    }

    private fun fileParameters(file: File): Map<String,String> {
        return mapOf(
            "path"      to file.absolutePath,
            "path-unix" to file.invariantSeparatorsPath,
            "dir"       to file.directory.absolutePath,
            "dir-unix"  to file.directory.invariantSeparatorsPath,
            "file"      to file.name,
            "name"      to file.nameWithoutExtension,
            "ext"       to file.extension,
        )
    }

    private fun homeParameters(): Map<String,String> {
        return mapOf(
            "home"      to Paths.userHome.pathString,
            "home-unix" to Paths.userHome.invariantPath,
        )
    }

    /** replace the variables of [text] found in [params]; any other `${...}` stays as it is. */
    private fun bind(text: String, params: Map<String,String>): String {
        if( text.isEmpty() ) return text
        return PATTERN_VARIABLE.replace(text) { params[it.groupValues[1]] ?: it.value }
    }

    private fun String.hasFileVariable(): Boolean =
        PATTERN_VARIABLE.findAll(this).any { it.groupValues[1] in FILE_VARIABLES }

    fun toCommand(): Command {
        return Command(workingDirectory=workingDirectory)
            .appendArguments(commandPrefix)
            .appendRaw(path)
            .appendArguments(argument)
            .also { if(it.isEmpty()) throw IllegalArgumentException("msg.error.no.command".message().format(title)) }
    }

    fun prevCommands(): List<Command> = toCommands(commandPrev)

    fun nextCommands(): List<Command> = toCommands(commandNext)

    private fun toCommands(script: String): List<Command> {
        return script
            .tokenize("\n\r")
            .map { Command(workingDirectory=workingDirectory).appendArguments(it) }
            .filterNot { it.isEmpty() }
    }

    override fun toString(): String {
        return "$title -> ${toCommand()}"
    }

}

/**
 * append [cli] split into arguments.
 *
 * the arguments keep their quotes. Windows programs take them off themselves, elsewhere they would reach
 * the program as part of the argument.
 */
private fun Command.appendArguments(cli: String): Command {
    if( Platforms.isWindows ) return append(cli)
    val start = command.size
    append(cli)
    for( i in start until command.size ) command[i] = command[i].unquote()
    return this
}

/** [this] without its quotes, the way a shell reads it: `--out="a b"` becomes `--out=a b`. */
internal fun String.unquote(): String {
    val buf = StringBuilder(length)
    var quote: Char? = null
    var i = 0
    while( i < length ) {
        val c = this[i]
        when {
            c == '\\' && quote != '\'' && getOrNull(i + 1) == '"' -> { buf.append('"'); i++ }
            quote == null && (c == '"' || c == '\'') -> quote = c
            c == quote -> quote = null
            else -> buf.append(c)
        }
        i++
    }
    return buf.toString()
}
