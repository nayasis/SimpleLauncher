package io.github.nayasis.simplelauncher.model.entity

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Convert
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.LargeText
import io.github.nayasis.simplelauncher.model.converter.MapConverter
import io.github.nayasis.simplelauncher.model.converter.PersonConverter

@Entity("TB_DEPARTMENT")
data class Department(

    @Id
    @Column(length = 10)
    var tenantId: String = "",

    @Id
    @Column(length = 10)
    var deptId: String = "",

    @Column(length = 100)
    var name: String = "",

    @Column
    @Convert(PersonConverter::class)
    @LargeText
    var person: Person? = null,

    @Column(length = 2000)
    @Convert(MapConverter::class)
    var attribute: Map<String, Any?>? = null,

) {

    override fun equals(other: Any?): Boolean {
        return (other is Department)
                && this.tenantId == other.tenantId
                && this.deptId   == other.deptId
    }
}