package io.github.nayasis.simplelauncher.model.exposed.entity

import com.dshatz.exposed_crud.Entity
import com.dshatz.exposed_crud.Id
import com.dshatz.exposed_crud.JsonFormat
import com.dshatz.exposed_crud.LargeText
import com.dshatz.exposed_crud.Varchar
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

    @com.dshatz.exposed_crud.Json("default")
    var person: Person? = null,
) {

    override fun equals(other: Any?): Boolean {
        return (other is ExpDepartment)
            && this.tenantId == other.tenantId
            && this.deptId == other.deptId
    }
}

@JsonFormat("default")
fun jsonformat(): kotlinx.serialization.json.Json {
    return kotlinx.serialization.json.Json {
        prettyPrint = true
    }
}