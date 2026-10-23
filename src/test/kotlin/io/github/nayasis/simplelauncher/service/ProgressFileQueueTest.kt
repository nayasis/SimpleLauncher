package io.github.nayasis.simplelauncher.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

internal class ProgressFileQueueTest {

    @Test
    fun pendingDuplicateOfADoneFileCanBeRemoved() {
        val file = File("a.txt")
        val queue = ProgressFileQueue(listOf(file, File("b.txt"), file))

        queue.next()

        queue.removePending(ProgressFileQueueItem(file)) shouldBe true
        queue.pendingItems() shouldBe listOf(ProgressFileQueueItem(File("b.txt")))
    }

    @Test
    fun doneFileCannotBeRemoved() {
        val file = File("a.txt")
        val queue = ProgressFileQueue(listOf(file, File("b.txt")))

        queue.next()

        queue.removePending(ProgressFileQueueItem(file)) shouldBe false
        queue.size shouldBe 2
    }

}
