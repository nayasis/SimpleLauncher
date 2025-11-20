package io.github.nayasis.simplelauncher.model.entity

import kotlinx.serialization.Serializable

@Serializable
data class Person(
    var name:    String = "",
    var age:     Int    = 0,
    var address: String = "",
)