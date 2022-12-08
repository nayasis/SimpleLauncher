package com.github.nayasis.simplelauncher

import com.github.nayasis.kotlin.basica.net.Networks
import com.github.nayasis.kotlin.javafx.spring.SpringFxApp
import com.github.nayasis.simplelauncher.common.BootLogger
import com.github.nayasis.simplelauncher.view.Main
import com.github.nayasis.simplelauncher.view.Splash
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tornadofx.launch
import java.util.*

private val logger = KotlinLogging.logger {}

@SpringBootApplication
//@Configuration
//@Import(value=[
//    AopAutoConfiguration::class,
//    DataSourceAutoConfiguration::class,
//    HibernateJpaAutoConfiguration::class,
//    JdbcTemplateAutoConfiguration::class,
//    JpaRepositoriesAutoConfiguration::class,
//    JtaAutoConfiguration::class,
//    JacksonAutoConfiguration::class,
//    PersistenceExceptionTranslationAutoConfiguration::class,
//    PropertyPlaceholderAutoConfiguration::class,
//    SqlInitializationAutoConfiguration::class,
//    PersistenceExceptionTranslationAutoConfiguration::class,
//    TransactionAutoConfiguration::class,
//])
class Simplelauncher: SpringFxApp(Main::class) {

    @Value("\${simplalauncher.locale:}")
    var locale: String = ""

    private var bootLogger: BootLogger? = BootLogger()

    override fun onStart(command: CommandLine) {
        Locale.setDefault(Locale.forLanguageTag(locale))
        bootLogger?.close()
        bootLogger = null
    }

    override fun setInitializers(): List<ApplicationContextInitializer<*>>? {
        return bootLogger?.getInitializer()?.let { listOf(it) }
    }

}

fun main(args: Array<String>) {
    Networks.trustAllCerts()
    SpringFxApp.run{
        loadMessage("/message/**.prop")
        loadDefaultIcon("/image/icon/favicon.png")
        setPreloader(Splash::class)
    }
    launch<Simplelauncher>(*args)
}