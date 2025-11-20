package io.github.nayasis.simplelauncher.model.exposed.entity

import com.dshatz.exposed_crud.Column
import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.Json
import com.dshatz.exposed_crud.Varchar
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.simplelauncher.model.entity.Person

@Entity
data class ExpDepartment(

    @Id
    @Varchar(length = 10)
    var tenantId: String = "",

    @Id
    @Varchar(length = 10)
    var deptId: String = "",

    @Varchar(length = 100)
    var name: String = "",

    @Json("default")
    var person: Person? = null,

    @Column("attribute")
    @Varchar(2000)
    var attributeJson: String? = null,

) {

    override fun equals(other: Any?): Boolean {
        return (other is ExpDepartment)
            && this.tenantId == other.tenantId
            && this.deptId == other.deptId
    }
}

var ExpDepartment.attribute: Map<String, Any>?
    get() = attributeJson?.let { Reflector.toObject<Map<String, Any>>(it) }
    set(value) {
        attributeJson = value?.let { Reflector.toJson(it) }
    }