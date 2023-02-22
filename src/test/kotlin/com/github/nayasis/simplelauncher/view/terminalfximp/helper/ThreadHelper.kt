package com.github.nayasis.simplelauncher.view.terminalfximp.helper

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

object ThreadHelper {
    private val uiSemaphore = Semaphore(1)
    private val singleExecutorService = Executors.newSingleThreadExecutor()

    // Runs task in JavaFX Thread
    fun runActionLater(runnable: Runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run()
        } else {
            try {
                uiSemaphore.acquire()
                Platform.runLater {
                    try {
                        runnable.run()
                        releaseUiSemaphor()
                    } catch (e: Exception) {
                        releaseUiSemaphor()
                        throw RuntimeException(e)
                    }
                }
            } catch (e: Exception) {
                releaseUiSemaphor()
                throw RuntimeException(e)
            }
        }
    }

    private fun releaseUiSemaphor() {
        singleExecutorService.submit { uiSemaphore.release() }
    }

    fun runActionLater(runnable: Runnable, force: Boolean) {
        if (force) {
            Platform.runLater(runnable)
        } else {
            runActionLater(runnable)
        }
    }

    fun start(runnable: Runnable?) {
        val thread = Thread(runnable)
        thread.start()
    }

    fun sleep(millis: Int) {
        try {
            Thread.sleep(millis.toLong())
        } catch (e: InterruptedException) {
//            e.printStackTrace();
        }
    }

    fun awaitLatch(countDownLatch: CountDownLatch) {
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun stopExecutorService() {
        if (!singleExecutorService.isShutdown) {
            singleExecutorService.shutdown()
        }
    }
}
