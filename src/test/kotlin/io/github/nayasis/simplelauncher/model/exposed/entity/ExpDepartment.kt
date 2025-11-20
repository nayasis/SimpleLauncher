package io.github.nayasis.simplelauncher.model.exposed.entity

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

object ExpDepartmentTable : Table("TB_EXP_DEPARTMENT") {

    val tenantId = varchar("tenant_id", length = 64)
    val deptId   = integer("dept_id")
    val name     = varchar("name", length = 255)
    val person   = varchar("person", length = 255).nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(tenantId, deptId)
}

data class ExpDepartment(
    val tenantId: String,
    val deptId: Int,
    val name: String,
    val person: String?,
) {
    companion object Dao {

        fun insert(
            tenantId: String,
            deptId: Int,
            name: String,
            person: String?,
        ): ExpDepartment {
            ExpDepartmentTable.insert {
                it[ExpDepartmentTable.tenantId] = tenantId
                it[ExpDepartmentTable.deptId] = deptId
                it[ExpDepartmentTable.name] = name
                it[ExpDepartmentTable.person] = person
            }
            return ExpDepartment(
                tenantId = tenantId,
                deptId = deptId,
                name = name,
                person = person,
            )
        }

        fun find(
            tenantId: String,
            deptId: Int,
        ): ExpDepartment {
            return ExpDepartmentTable
                .selectAll()
                .where { ExpDepartmentTable.tenantId eq tenantId }
                .singleOrNull()?.toExpDepartment()
                ?: throw NoSuchElementException("No department found for tenantId='$tenantId', deptId='$deptId'")
        }

        private fun ResultRow.toExpDepartment() = ExpDepartment(
            tenantId = this[ExpDepartmentTable.tenantId],
            deptId = this[ExpDepartmentTable.deptId],
            name = this[ExpDepartmentTable.name],
            person = this[ExpDepartmentTable.person],
        )
    }
}
