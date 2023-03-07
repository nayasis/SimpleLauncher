package com.github.nayasis.simplelauncher.service

import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.tokenize
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.property.InsetProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context.Companion.config
import com.github.nayasis.simplelauncher.common.Context.Companion.main
import com.github.nayasis.simplelauncher.jpa.entity.Link
import com.github.nayasis.simplelauncher.view.Terminal
import javafx.stage.Stage
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

    fun run(link: Link, files: Collection<File>? = null) {

        link.title?.let { config.historyKeyword.add(it) }

        linkService.save( link.apply {
            lastExecDate = LocalDateTime.now()
            executeCount++
        })
        main.tableMain.refresh()

        if( files.isNullOrEmpty() ) {
            runLater { run(LinkCommand(link), false) }
        } else if( files.size == 1 ) {
            runLater { run(LinkCommand(link, files.first())) }
        } else {
            if( link.eachExecution ) {
                val parentInset = main.currentStage?.let { InsetProperty(it) }
                if( ! link.showConsole ) {
                    Dialog.progress(link.title) {
                        setStageToMiddle(it.stage, parentInset)
                        files.forEachIndexed { index, file ->
                            it.updateProgress(index + 1,files.size)
                            it.updateMessage(file.name)
                            it.updateSubMessageAsProgress()
                            run(LinkCommand(link, file),wait=true, closeConsoleWhenDone = true)
                        }
                    }
                } else {
                    val progress = Dialog.progress(link.title)
                    setStageToMiddle(progress.stage, parentInset)
                    files.forEachIndexed { index, file ->
                        progress.updateProgress(index + 1, files.size)
                        progress.updateMessage(file.name)
                        progress.updateSubMessageAsProgress()
                        run(LinkCommand(link, file), wait = true, closeConsoleWhenDone = true)
                    }
                    progress.close()
                }
            } else {
                runLater { run(LinkCommand(link,files),false) }
            }
        }

    }

    private fun run(linkCmd: LinkCommand, wait: Boolean = false, closeConsoleWhenDone: Boolean = false) {
        with(linkCmd) {
            commandPrev.tokenize("\n").forEach { cmd -> run(Command(cmd,workingDirectory),true, false) }
            toCommand().let {cmd ->
                main.printCommand("$cmd")
                run(cmd, wait, showConsole, closeConsoleWhenDone)
            }
            commandNext.tokenize("\n").forEach { cmd -> run(Command(cmd,workingDirectory),true, false) }
        }
    }

    private fun run(command: Command, wait: Boolean, showConsole: Boolean = false, closeConsoleWhenDone: Boolean = false) {
        if( command.isEmpty() ) return
        logger.debug { ">> command : $command" }
        if( showConsole ) {
            val terminal = Terminal(command, onFail = { e ->
                throw RuntimeException("msg.err.003".message().format("$command")).apply { this.stackTrace = e.stackTrace }
            }, onDone = {
                if(closeConsoleWhenDone) {
                    runLater { it.close() }
                }
            })
            if(wait) {
                terminal.showAndWait()
            } else {
                terminal.show()
            }
        } else {
            try {
                command.run().also { if(wait) it.waitFor() }
            } catch (e: Exception) {
                throw RuntimeException("msg.err.003".message().format("$command")).apply { this.stackTrace = e.stackTrace }
            }
        }
    }

    private fun setStageToMiddle(stage: Stage, parentInset: InsetProperty?) {
        parentInset?.let {
            stage.x = it.x + it.width  / 2 - stage.width  / 2
            stage.y = it.y + it.height / 2 - stage.height / 2
        }
    }

}

