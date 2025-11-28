package io.github.nayasis.simplelauncher.model.entity

import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.LargeText
import com.dshatz.exposed_crud.Varchar
import io.github.nayasis.simplelauncher.model.converter.MapConverter
import io.github.nayasis.simplelauncher.model.converter.PersonConverter

@Entity("TB_DEPARTMENT")
data class Department(

    @Id
    @Varchar(length = 10)
    var tenantId: String = "",

    @Id
    @Varchar(length = 10)
    var deptId: String = "",

    @Varchar(length = 100)
    var name: String = "",

    @Convert(PersonConverter::class)
    @LargeText
    var person: Person? = null,

    @Convert(MapConverter::class)
    @Varchar(2000)
    var attribute: Map<String, Any?>? = null,

) {

    override fun equals(other: Any?): Boolean {
        return (other is Department)
                && this.tenantId == other.tenantId
                && this.deptId   == other.deptId
    }
}