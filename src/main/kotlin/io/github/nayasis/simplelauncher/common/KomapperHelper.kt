package io.github.nayasis.simplelauncher.common

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.javafx.app.FxApp.Companion.environment
import io.github.oshai.kotlinlogging.KotlinLogging
import org.komapper.core.dsl.query.Query
import org.komapper.core.spi.DataTypeConverter
import org.komapper.jdbc.JdbcDataType
import org.komapper.jdbc.JdbcDataTypeProvider
import org.komapper.jdbc.JdbcDataTypeProviders
import org.komapper.jdbc.JdbcDataTypeProxy
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDialects
import org.komapper.tx.core.EmptyTransactionProperty
import org.komapper.tx.core.TransactionAttribute
import org.komapper.tx.core.TransactionOperator
import org.komapper.tx.core.TransactionProperty
import kotlin.reflect.KType

private val logger = KotlinLogging.logger {}

object KomapperHelper {

    lateinit var database: JdbcDatabase

    private fun createJdbcDataTypeProviderWithConverters(
        driver: String,
        customConverters: List<DataTypeConverter<*,*>>?,
    ): JdbcDataTypeProvider {

        val baseProvider = JdbcDataTypeProviders.get(driver)
        val convertersByType = customConverters?.associateBy { it.exteriorType }

        return object : JdbcDataTypeProvider {
            override fun <T : Any> get(type: KType): JdbcDataType<T>? {
                @Suppress("UNCHECKED_CAST")
                val converter = convertersByType?.get(type) as DataTypeConverter<T, Any>?
                return if (converter == null) {
                    baseProvider.get(type)
                } else {
                    val dataType: JdbcDataType<Any> = baseProvider.get(converter.interiorType)
                        ?: error("The dataType is not found for the type \"${converter.interiorType}\".")
                    JdbcDataTypeProxy(converter, dataType)
                }
            }
        }
    }

    fun connectDatabase(
        url             : String? = null,
        user            : String? = null,
        password        : String? = null,
        customConverters: List<DataTypeConverter<*,*>>? = null,
    ) {

        val dialect = JdbcDialects.getByUrl(url ?: environment["simplelauncher.datasource.url"] ?: "")
        val customProvider = createJdbcDataTypeProviderWithConverters(dialect.driver, customConverters)

        database = runCatching { JdbcDatabase(
            url      = url      ?: environment["simplelauncher.datasource.url"]      ?: "",
            user     = user     ?: environment["simplelauncher.datasource.user"]     ?: "",
            password = password ?: environment["simplelauncher.datasource.password"] ?: "",
            dataTypeProvider = customProvider
        ) }.onFailure { logger.error(it) }.getOrThrow()
    }
    
    fun <T> Query<T>.runQuery(): T {
        return database.runQuery(this)
    }

    fun <R> withTransaction(
        transactionAttribute: TransactionAttribute = TransactionAttribute.REQUIRED,
        transactionProperty: TransactionProperty = EmptyTransactionProperty,
        block: (TransactionOperator) -> R
    ): R {
        return database.withTransaction(transactionAttribute, transactionProperty, block)
    }

}