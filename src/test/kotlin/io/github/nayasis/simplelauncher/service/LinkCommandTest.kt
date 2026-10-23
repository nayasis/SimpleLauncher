package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.Paths
import io.github.nayasis.kotlin.basica.core.io.pathString
import io.github.nayasis.simplelauncher.model.Link
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class LinkCommandTest {

    @TempDir
    lateinit var dir: File

    private fun file(name: String) = File(dir, name).apply { writeText("") }

    @Test
    fun filesAreAppendedWhenNoFieldUsesThem() {
        val a = file("a.txt")
        val b = file("b.txt")

        val command = LinkCommand(Link(argument = "-v"), listOf(a, b))

        command.argument shouldBe "-v \"${a.path}\" \"${b.path}\""
    }

    @Test
    fun argumentRepeatsPerFile() {
        val a = file("a.txt")
        val b = file("b.txt")

        val command = LinkCommand(Link(argument = "-i \"\${path}\""), listOf(a, b))

        command.argument shouldBe "-i \"${a.absolutePath}\" -i \"${b.absolutePath}\""
    }

    @Test
    fun fileVariableInPreviousCommandKeepsTheArgument() {
        val a = file("a.txt")

        val command = LinkCommand(Link(argument = "--run", commandPrev = "copy \"\${path}\" backup\necho done"), a)

        command.argument shouldBe "--run"
        command.commandPrev shouldBe "copy \"${a.absolutePath}\" backup\necho done"
    }

    @Test
    fun previousCommandLinesRepeatPerFile() {
        val a = file("a.txt")
        val b = file("b.txt")

        val command = LinkCommand(Link(commandPrev = "copy \${file} backup"), listOf(a, b))

        command.commandPrev shouldBe "copy a.txt backup\ncopy b.txt backup"
    }

    @Test
    fun homeIsBoundWithoutFiles() {
        val command = LinkCommand(Link(argument = "\${home}"))

        command.argument shouldBe Paths.userHome.pathString
    }

    @Test
    fun unknownVariablesStay() {
        val a = file("a.txt")

        val command = LinkCommand(Link(argument = "\${name}", commandPrev = "echo \${JAVA_HOME}"), a)

        command.argument shouldBe "a"
        command.commandPrev shouldBe "echo \${JAVA_HOME}"
    }

    @Test
    fun pathFoundByRelativePathIsRun() {
        val target = file("tool.exe")
        // the stored path is gone, the relative one still leads to the file
        val link = Link(path = File(dir, "moved/tool.exe").path, relativePath = target.absolutePath)

        val command = LinkCommand(link)

        command.path shouldBe target.toPath().pathString
        command.toCommand().toString() shouldContain "tool.exe"
        command.toCommand().toString() shouldNotContain "moved"
    }

    @Test
    fun unquoteReadsQuotesLikeAShell() {
        "\"/home/u/a b.txt\"".unquote() shouldBe "/home/u/a b.txt"
        "--out=\"a b\"".unquote() shouldBe "--out=a b"
        "'it''s'".unquote() shouldBe "its"
        "\"say \\\"hi\\\"\"".unquote() shouldBe "say \"hi\""
        "plain".unquote() shouldBe "plain"
    }

}
