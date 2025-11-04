package io.github.nayasis.simplelauncher.model

import io.github.nayasis.kotlin.basica.core.collection.flattenKeys
import io.github.nayasis.kotlin.basica.core.collection.toObject
import io.github.nayasis.kotlin.basica.core.string.toResource
import io.github.nayasis.kotlin.basica.core.url.toInputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

private val logger = KotlinLogging.logger {}

class YmlTest {
    @Test
    fun `load YML through extension function`() {

//        val config = "test.yml".loadYml<TestConfig>()
        val config = "test.yml".loadYml<LinkedHashMap<String,Any>>()?.flattenKeys()

        LoggerFactory.getILoggerFactory()



        logger.debug { config }
    }
}

data class TestConfig(
    var user: User?  = null,
    var test: String = "",
)

data class User(
    var name: String? = null,
    var age: Int?     = null,
)

inline fun <reified T> String.loadYml(): T? {
    return this.toResource()?.toInputStream()?.use {
        Yaml().loadAs(it, LinkedHashMap::class.java).toObject<T>()
    }
}