package com.github.nayasis.simplelauncher.view.jediterm

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.StopWatch
import com.jediterm.terminal.ProcessTtyConnector
import java.nio.charset.Charset

class PureProcessTtyConnector(process: Process, charset: Charset = Charset.forName(Platforms.os.charset) ): ProcessTtyConnector(
    process, charset
) {

    val stopWatcher = StopWatch()

    override fun getName(): String {
        return "local"
    }

    override fun isConnected(): Boolean {
        println(">> check is connected !")
        return process.isAlive
    }
}