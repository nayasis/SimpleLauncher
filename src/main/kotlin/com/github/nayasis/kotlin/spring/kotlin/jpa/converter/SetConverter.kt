package com.github.nayasis.kotlin.spring.kotlin.jpa.converter

import javax.persistence.AttributeConverter

class SetConverter<T>: AttributeConverter<Set<T>,T> {

    override fun convertToDatabaseColumn(items: Set<T>?): T {
        TODO("Not yet implemented")
    }

    override fun convertToEntityAttribute(string: T?): Set<T> {
        TODO("Not yet implemented")
    }

}