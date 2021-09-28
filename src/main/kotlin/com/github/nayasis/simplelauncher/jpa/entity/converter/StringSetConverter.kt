package com.github.nayasis.simplelauncher.jpa.entity.converter

import com.github.nayasis.kotlin.basica.reflection.Reflector
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class StringSetConverter: AttributeConverter<Set<String>,String> {

    override fun convertToDatabaseColumn(java: Set<String>?): String {
        return Reflector.toJson(java)
    }

    override fun convertToEntityAttribute(database: String?): Set<String> {
        return Reflector.toObject(database)
    }

}