package com.github.nayasis.kotlin.basica.base

import com.github.nayasis.kotlin.basica.toDate
import com.github.nayasis.kotlin.basica.toLocalDate
import com.github.nayasis.kotlin.basica.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SerializableTest {

    @Test
    fun test() {

        val project = Project("sample","java")

        println( Json.encodeToString(project) )

        val p2 = Json.decodeFromString<Project>("""{"name":"sample2","localdatetime":"2040-01-01 02:43:22","localdate":"2040-01-01 02:43:22","date":"2040-01-01"}""" )

        println( p2 )

    }

}

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return decoder.decodeString().toLocalDateTime()
    }
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        return encoder.encodeString(value.toString())
    }
}

@Serializer(forClass = LocalDateTime::class)
object LocalDateSerializer : KSerializer<LocalDate> {
    override fun deserialize(decoder: Decoder): LocalDate {
        return decoder.decodeString().toLocalDate()
    }
    override fun serialize(encoder: Encoder, value: LocalDate) {
        return encoder.encodeString(value.toString())
    }
}

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override fun deserialize(decoder: Decoder): Date {
        return decoder.decodeString().toDate()
    }
    override fun serialize(encoder: Encoder, value: Date) {
        return encoder.encodeString(value.toString())
    }
}


@Serializable
data class Project(
    val name: String,
    val language: String = "kotlin",
    @Serializable(with = LocalDateTimeSerializer::class)
    val localdatetime: LocalDateTime? = null,
    @Serializable(with = LocalDateSerializer::class)
    val localdate: LocalDate? = null,
    @Serializable(with = DateSerializer::class)
    val date: Date? = null,
)

