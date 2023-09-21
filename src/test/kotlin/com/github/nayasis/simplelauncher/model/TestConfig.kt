package com.github.nayasis.simplelauncher.model

import com.github.nayasis.kotlin.basica.core.collection.flattenKeys
import com.github.nayasis.kotlin.basica.core.collection.toObject
import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.core.url.toInputStream
import mu.KotlinLogging
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