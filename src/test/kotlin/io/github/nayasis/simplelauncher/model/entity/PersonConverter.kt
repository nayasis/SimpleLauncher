package io.github.nayasis.simplelauncher.model.entity

import io.github.nayasis.kotlin.basica.reflection.Reflector
import org.komapper.core.spi.DataTypeConverter
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class PersonConverter: DataTypeConverter<Person, String> {
    override val exteriorType: KType = typeOf<Person>()
    override val interiorType: KType = typeOf<String>()

    override fun wrap(interior: String): Person {
        return Reflector.toObject<Person>(interior)
    }

    override fun unwrap(exterior: Person): String {
        return Reflector.toJson(exterior)
    }
}