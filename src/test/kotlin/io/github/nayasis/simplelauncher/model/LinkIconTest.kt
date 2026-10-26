package io.github.nayasis.simplelauncher.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.github.nayasis.simplelauncher.common.ICON_NEW
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream

class LinkIconTest : FunSpec({

    test("stored legacy icon should display without importing again") {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(ICON_NEW) }
            output.toByteArray()
        }
        val link = Link(icon = bytes)

        link.iconImage.shouldNotBeNull()
        (link.icon === bytes) shouldBe true
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


