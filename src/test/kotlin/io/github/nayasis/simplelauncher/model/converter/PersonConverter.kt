package io.github.nayasis.simplelauncher.model.converter

import com.dshatz.exposed_crud.interfaces.AttributeConverter
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.simplelauncher.model.entity.Person

class PersonConverter : AttributeConverter<Person?, String?> {
    override fun convertToDatabaseColumn(entityData: Person?): String? {
        return entityData?.let { Reflector.toJson(it) }
    }
    override fun convertToEntityAttribute(dbData: String?): Person? {
        return dbData?.let { Reflector.toObject<Person>(it) }
    }
}