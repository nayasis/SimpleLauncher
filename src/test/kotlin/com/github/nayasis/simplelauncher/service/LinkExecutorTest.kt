package com.github.nayasis.simplelauncher.service

import com.pty4j.PtyProcessBuilder
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Ignored
class LinkExecutorTest: StringSpec({

    "run simple" {
        val process = PtyProcessBuilder(arrayOf(
            "python",
            "e:\\download\\3ds\\3dsconv\\3dsconv.py",
            "--boot9=boot9.bin",
            "--overwrite",
            "--output=\"\\\\NAS2\\emul\\image\\3DS\\3D Ecco the Dolphin (m2)(en)\"",
            "\"\\\\NAS2\\emul\\image\\3DS\\3D Ecco the Dolphin (m2)(en)\\3D Ecco the Dolphin (USA) (eShop).3ds\""
        )).setDirectory("c:/app/SimpleLauncherApp/3dsconv")
//            .setConsole(true)
//            .setConsole(true)
            .setWindowsAnsiColorEnabled(true)
            .setUseWinConPty(true)
        .start()

        // 출력 스트림 읽기
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println(line)
        }

        // 오류 스트림 읽기
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        var errorLine: String?
        while (errorReader.readLine().also { errorLine = it } != null) {
            System.err.println(errorLine)
        }

        // 프로세스 종료 대기
        val exitCode = process.waitFor()
        println("Exit Code: $exitCode")


    }

    "run command" {

        val command = listOf(
            "c:\\app\\SimpleLauncherApp\\3dsconv\\3dsconv.exe",
            "--boot9=boot9.bin",
            "--overwrite",
            "--output=\"\\\\NAS2\\emul\\image\\3DS\\Made in Wario Gorgeus (nintendo)(T-ko 0.74)\"",
            "\\\\NAS2\\emul\\image\\3DS\\Made in Wario Gorgeus (nintendo)(T-ko 0.74)\\Made in Wario Gorgeus (ko).3ds"
        )
        val processBuilder = ProcessBuilder(command)
            .directory(File("c:\\app\\SimpleLauncherApp\\3dsconv"))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        println("Exit Code: $exitCode")

    }

})