package io.github.nayasis.simplelauncher.service

import io.github.nayasis.simplelauncher.common.ExposedHelper
import io.github.nayasis.simplelauncher.common.ExposedHelper.tx
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.model.LinkTable
import io.github.nayasis.simplelauncher.model.repo
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.junit.jupiter.api.Test

internal class LinkServiceSuggestionTest {

    @Test
    fun groupSuggestionUsesGlobalFrequencyAndExcludedTokens() {
        ExposedHelper.connectDatabase(
            url = "jdbc:h2:mem:link-service-suggestion;DB_CLOSE_DELAY=-1",
            user = "user",
            password = "1234",
        )

        tx {
            LinkTable.deleteAll()
            LinkTable.repo.save(Link(title = "A", group = hashSetOf("alpha", "beta")))
            LinkTable.repo.save(Link(title = "B", group = hashSetOf("alpha", "gamma")))
            LinkTable.repo.save(Link(title = "C", group = hashSetOf("beta", "gamma")))
        }

        val service = LinkService()
        service.loadAll()

        service.groupTokenSuggestions() shouldBe listOf("alpha", "beta", "gamma")
        service.groupTokenSuggestions(listOf("alpha")) shouldBe listOf("beta", "gamma")
        service.groupTokenSuggestions(listOf("alpha", "beta")) shouldBe listOf("gamma")
    }
}

