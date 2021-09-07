package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.path.directory
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.etc.Platforms
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.Reader
import java.util.concurrent.Executors

class ApacheExecTest {

    @Test
    fun test() {

//        val cd = "d:/download/test/cso"
//        val command = "${cd}/CisoPlus.exe -com -l9 ${cd}/disc.iso ${cd}/disc.cso"

        val cd = "d:/download/test/chd"
        val command = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"

        val executor = DefaultExecutor().apply {
            workingDirectory = cd.toFile().directory
        }
        val resultHandler = DefaultExecuteResultHandler()

        val inStream = PipedInputStream()
        val outStream = PipedOutputStream(inStream)
        val pumpStream = PumpStreamHandler(outStream)

        executor.streamHandler = pumpStream

        val thread = Thread {
            print( BufferedReader(InputStreamReader(inStream,Platforms.os.charset)) )
        }

        Executors.newSingleThreadExecutor().execute(thread)

        executor.execute(CommandLine.parse(command), resultHandler)

        resultHandler.waitFor()

    }

    @Test
    fun test2() {

        val cd = "d:/download/test/chd"
        val command = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"

        val process = ProcessBuilder(command.split(" ")).start()

        val threadPool = Executors.newFixedThreadPool(5)

        threadPool.execute {
            print( BufferedReader(InputStreamReader(process.inputStream,Platforms.os.charset)) )
        }

        threadPool.execute {
            print( BufferedReader(InputStreamReader(process.errorStream,Platforms.os.charset)) )
        }

        process.waitFor()

    }

    private fun print(reader: Reader) {
        try {
            var nRead: Int
            val data = CharArray(1 * 1024)
            while (reader.read(data, 0, data.size).also { nRead = it } != -1) {
                val sb = StringBuilder(nRead)
                sb.append(data, 0, nRead)
                print(sb)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}