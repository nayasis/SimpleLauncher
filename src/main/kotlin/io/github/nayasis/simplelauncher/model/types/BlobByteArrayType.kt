package io.github.nayasis.simplelauncher.model.types

import org.komapper.jdbc.spi.JdbcUserDefinedDataType
import java.sql.Blob
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.typeOf

class BlobByteArrayType : JdbcUserDefinedDataType<BlobByteArray> {

    override val name     = "BLOB"
    override val type     = typeOf<BlobByteArray>()
    override val jdbcType = JDBCType.BLOB

    override fun getValue(rs: ResultSet, index: Int): BlobByteArray? {
        return rs.getBlob(index)?.toBlobByteArray()
    }

    override fun getValue(rs: ResultSet, columnLabel: String): BlobByteArray? {
        return rs.getBlob(columnLabel)?.toBlobByteArray()
    }

    override fun setValue(ps: PreparedStatement, index: Int, value: BlobByteArray) {
        ps.setBytes(index, value.value)
    }

    override fun toString(value: BlobByteArray): String {
        return value.toString()
    }

    private fun Blob.toBlobByteArray(): BlobByteArray {
        return try {
            BlobByteArray(getBytes(1, length().toInt()))
        } finally {
            free()
        }
    }

}
