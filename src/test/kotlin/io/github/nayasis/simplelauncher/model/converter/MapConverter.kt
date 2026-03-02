package io.github.nayasis.simplelauncher.model.converter

import com.dshatz.exposed_crud.interfaces.AttributeConverter
import io.github.nayasis.kotlin.basica.reflection.Reflector

class MapConverter : AttributeConverter<Map<String, Any?>?, String?> {
    override fun convertToDatabaseColumn(entityData: Map<String, Any?>?): String? {
        return entityData?.let { Reflector.toJson(it) }
    }
    override fun convertToEntityAttribute(dbData: String?): Map<String, Any?>? {
        return dbData?.let { Reflector.toObject<Map<String, Any>>(it) }
    }
}