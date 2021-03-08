package com.github.nayasis.kotlin.basica

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class SerializableTest {

    @Test
    fun test() {

        val project = Project("sample","java")

        println( Json.encodeToString(project) )

        val p2 = Json.decodeFromString<Project>("""{"name":"sample2"}""" )

        println( p2 )



    }

}

@Serializable
data class Project(
    val name: String,
    val language: String = "kotlin",
//    val regDt: LocalDateTime = now(),
)

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    override fun deserialize(decoder: Decoder): LocalDateTime {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString()
    }

//    override fun load(input: KInput): LocalDateTime {
//        return LocalDateTime.parse(input.readStringValue(), DateTimeFormatter.ISO_DATE_TIME)
//    }
//
//    override fun save(output: KOutput, obj: LocalDateTime) {
//        output.writeStringValue(obj.format(DateTimeFormatter.ISO_DATE_TIME))
//    }

}