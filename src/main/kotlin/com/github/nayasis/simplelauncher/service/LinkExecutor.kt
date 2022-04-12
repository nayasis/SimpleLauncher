package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context.Companion.main
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.view.CircularFifoSet
import com.github.nayasis.simplelauncher.view.terminal.Terminal
import mu.KotlinLogging
import org.springframework.stereotype.Service
import tornadofx.runLater
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger{}

@Service
class LinkExecutor(
    private val linkService: LinkService
) {

    val history = CircularFifoSet<String>(20)

    fun run(link: Link, files: Collection<File>? = null) {

        link.title?.let { history.add(it) }

        linkService.save( link.apply {
            lastExecDate = LocalDateTime.now()
            executeCount++
        })
        main.tableMain.refresh()

        if( files.isNullOrEmpty() ) {
            runLater { run(LinkCommand(link)) }
        } else if( files.size == 1 ) {
            runLater { run(LinkCommand(link,files.first())) }
        } else {
            if( link.eachExecution ) {
                if( ! link.showConsole ) {
                    Dialog.progress(link.title) {
                        files.forEachIndexed { index, file ->
                            updateProgress(index + 1L,files.size.toLong())
                            updateMessage(file.name)
                            run(LinkCommand(link, file),wait=true)
                        }
                    }
                } else {
                    val progress = Dialog.progress(link.title)
                    files.forEachIndexed { index, file ->
                        progress.updateProgress(index + 1, files.size)
                        progress.updateMessage(file.name)
                        val cmd = LinkCommand(link, file)
                        logger.debug { ">> command : $cmd" }
                        Terminal(cmd.toCommand(),
                            onStart = { ConfigService.stageTerminal?.bind(it) },
                            onSuccess = { runLater { it.close() } },
                            onFail = { throwable, it ->
                                runLater {
                                    Dialog.error(throwable)
                                    it.close()
                                }
                            },
                            onDone = { ConfigService.stageTerminal = StageProperty(it) }
                        ).showAndWait()
                    }
                    progress.close()
                }
            } else {
                runLater { run(LinkCommand(link,files),false) }
            }
        }

    }

    private fun run(linkCmd: LinkCommand, wait: Boolean = false) {
        with(linkCmd) {
            val command = toCommand()
            commandPrev.tokenize("\n").forEach { run(Command(it,workingDirectory),true,showConsole) }
            main.printCommand("$command")
            run(command, wait || showConsole, showConsole)
            commandNext.tokenize("\n").forEach { run(Command(it,workingDirectory),true,showConsole) }
        }
    }

    private fun run(command: Command, wait: Boolean, showConsole: Boolean = false) {
        if( command.isEmpty() ) return
        logger.debug { ">> command : $command" }
        if( showConsole ) {
            val terminal = Terminal(command)
            if(wait) {
                terminal.showAndWait()
            } else {
                terminal.show()
            }
        } else {
            command.run().also { if(wait) it.waitFor() }
        }
    }

}