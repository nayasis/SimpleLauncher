package io.github.nayasis.simplelauncher.model.entity

import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable

@KomapperEntity
@KomapperTable(name = "TB_LINK_TEST")
data class Department(
    @KomapperId
    @KomapperColumn
    var id: Int = 0,
    @KomapperColumn
    var name: String = "",
    @KomapperColumn(name = "person")
    var person: Person? = null,
)