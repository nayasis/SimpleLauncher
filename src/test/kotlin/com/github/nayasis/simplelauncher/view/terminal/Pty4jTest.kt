package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.io.Paths
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.simplelauncher.view.terminal.old.TerminalOld
import com.pty4j.PtyProcessBuilder
import com.pty4j.util.PtyUtil
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<Pty4j>(args)
}

class Pty4j: App() {
    override fun start(stage: Stage) {

        setLocalPtyLib()

        val builder = PtyProcessBuilder(arrayOf("cmd","/c","dir"))

        val process = builder.start()


//        Terminal(Command("C:/app/SimpleLauncherApp/NSC Builder/NSCB.bat")).showAndWait()
        TerminalOld(Command("cmd /c dir")).showAndWait()
        exitProcess(0)
    }

    private fun setLocalPtyLib() {
        if ("false".equals(System.getProperty(PtyUtil.PREFERRED_NATIVE_FOLDER_KEY))) {
            System.clearProperty(PtyUtil.PREFERRED_NATIVE_FOLDER_KEY);
        }
        else {
            System.setProperty(PtyUtil.PREFERRED_NATIVE_FOLDER_KEY, getBuiltNativeFolder().toString());
        }
    }

    fun getBuiltNativeFolder(): Path {
        return Paths.get("os").toAbsolutePath().normalize()
    }

}