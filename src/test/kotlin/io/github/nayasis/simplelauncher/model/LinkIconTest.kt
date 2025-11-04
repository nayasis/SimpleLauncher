package io.github.nayasis.simplelauncher.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import javafx.embed.swing.JFXPanel

class LinkIconTest : FunSpec({

    beforeSpec {
        // Initialize JavaFX toolkit once for image processing in tests
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater {
            JFXPanel() // triggers JavaFX initialization
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)
    }

    test("setIcon should load and encode various image formats") {
        val filenames = listOf(
            "test.gif",
            "test.ico",
            "test.jpeg",
            "test.png",
            "test.jpg",
        )

        filenames.forEach { name ->
            val url = this::class.java.getResource("/image/$name")
            url.shouldNotBeNull()
            val file = File(url.toURI())

            val link = Link()
            val image = link.setIcon(file)

            image.shouldNotBeNull()
            link.icon.shouldNotBeNull()
            // icon bytes should be non-empty
            (link.icon?.isNotEmpty() ?: false) shouldBe true
        }
    }
})


