package io.github.nayasis.simplelauncher.model

import io.github.nayasis.simplelauncher.common.KomapperHelper
import io.github.nayasis.simplelauncher.common.KomapperHelper.runQuery
import io.github.nayasis.simplelauncher.model.entity.Department
import io.github.nayasis.simplelauncher.model.entity.Person
import io.github.nayasis.simplelauncher.model.entity.PersonConverter
import io.github.nayasis.simplelauncher.model.entity.department
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull

private val logger = KotlinLogging.logger {}

class JsonMappingTest {

    @Test
    fun basic() {
        // 코드로 직접 PersonConverter 등록
        // KomapperHelper.createDatabase를 사용하여 컨버터를 등록하고 데이터베이스를 생성합니다.
        KomapperHelper.connectDatabase(
            url      = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            user     = "user",
            password = "1234",
            listOf(
                PersonConverter()
            )
        )

        QueryDsl.create(Meta.department).runQuery()

        val record = Department(
            name = "Department",
        ).apply {
            person = Person(
                name    = "Person",
                age     = 30,
                address = "Somewhere",
            )
        }

        val inserted = QueryDsl.insert(Meta.department).single(record).runQuery()

        logger.debug { ">> committed\n$inserted" }

        val read = QueryDsl.from(Meta.department).where { Meta.department.id eq inserted.id }.firstOrNull().runQuery()

        logger.debug { ">> read\n$read" }

    }

}