package io.github.nayasis.simplelauncher.model.exposed

import com.dshatz.exposed_crud.JsonFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule

/**
 * Map<String, Any>를 위한 Serializer
 * Any 타입을 JsonElement로 변환하여 직렬화
 */
object MapStringAnySerializer : KSerializer<Map<String, Any>> {
    private val json = Json { ignoreUnknownKeys = true }
    
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Map")

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        val jsonElement = buildJsonObject {
            value.forEach { (k, v) ->
                put(k, anyToJsonElement(v))
            }
        }
        // JsonElement를 문자열로 인코딩 (exposed의 json 함수가 이를 사용)
        encoder.encodeString(json.encodeToString(JsonElement.serializer(), jsonElement))
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        val jsonString = decoder.decodeString()
        val jsonElement = json.decodeFromString<JsonElement>(jsonString)
        return jsonElementToMap(jsonElement.jsonObject)
    }

    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                buildJsonObject {
                    (value as Map<String, Any?>).forEach { (k, v) ->
                        put(k, anyToJsonElement(v))
                    }
                }
            }
            is List<*> -> {
                buildJsonArray {
                    value.forEach { item ->
                        add(anyToJsonElement(item))
                    }
                }
            }
            is JsonElement -> value
            null -> JsonPrimitive("null")
            else -> {
                // 다른 타입은 toString으로 처리
                JsonPrimitive(value.toString())
            }
        }
    }

    private fun jsonElementToMap(jsonObject: JsonObject): Map<String, Any> {
        return jsonObject.entries.associate { (k, v) ->
            k to jsonElementToAny(v)
        }
    }

    private fun jsonElementToAny(element: JsonElement): Any {
        return when {
            element is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    else -> {
                        // 숫자나 불린 값은 content를 파싱
                        val content = element.content
                        when {
                            content == "true" -> true
                            content == "false" -> false
                            content.toLongOrNull() != null -> content.toLong()
                            content.toDoubleOrNull() != null -> content.toDouble()
                            else -> content
                        }
                    }
                }
            }
            element is JsonObject -> {
                element.entries.associate { (k, v) ->
                    k to jsonElementToAny(v)
                }
            }
            element.jsonArray != null -> {
                element.jsonArray.map { jsonElementToAny(it) }
            }
            else -> element
        }
    }
}

@JsonFormat("default")
fun jsonformat(): kotlinx.serialization.json.Json {
    return kotlinx.serialization.json.Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
}
