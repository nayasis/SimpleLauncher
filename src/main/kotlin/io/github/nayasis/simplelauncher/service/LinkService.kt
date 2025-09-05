package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.io.*
import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.javafx.misc.Desktop
import io.github.nayasis.kotlin.javafx.misc.set
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.database.DataSource.db
import io.github.nayasis.simplelauncher.model.Link
import io.github.nayasis.simplelauncher.model.link
import io.github.nayasis.simplelauncher.model.vo.JsonLink
import io.github.oshai.kotlinlogging.KotlinLogging
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.asc
import org.komapper.core.dsl.operator.count
import tornadofx.FileChooserMode
import tornadofx.SortedFilteredList
import tornadofx.asObservable
import tornadofx.runLater
import java.nio.file.Path
import java.util.*

private val logger = KotlinLogging.logger{}

class LinkService {

    val links = SortedFilteredList(mutableListOf<Link>().asObservable())

    fun save(link: Link, refreshTable: Boolean = true) {
        if(link.isNew) {
            links.add(link)
        }

        db.withTransaction {
            if(link.isNew) {
                db.runQuery{
                    QueryDsl.insert(Meta.link).single(link)
                }
            } else {
                db.runQuery{
                    QueryDsl.update(Meta.link).single(link)
                }
            }
        }

        if(refreshTable) {
            runLater {
                Context.main.tableMain.refresh()
            }
        }
    }

    fun importData(file: Path) {
        val jsonLinks = file.readText().let { Reflector.toObject<List<JsonLink>>(it) }.map { it.toLink() }
        jsonLinks.forEach { link ->
            db.runQuery(QueryDsl.insert(Meta.link).single(link))
        }
    }

    fun countAll(): Long {
        return db.runQuery(QueryDsl.from(Meta.link).select(count(Meta.link.id))) ?: 0
    }

    fun loadAll(worker: ((index: Int, link: Link) -> Unit)? = null) {
        var i = 0
        val links = LinkedList<Link>()

        db.runQuery(
            QueryDsl.from(Meta.link)
                .orderBy(Meta.link.title.asc())
        ).forEach { link ->
            links.add(link)
            worker?.invoke(++i, link)
        }

        this.links.run {
            clear()
            addAll(links)
        }
    }

    fun exportData(file: Path) {
        val dbLinks = db.runQuery(
            QueryDsl.from(Meta.link)
        ).map { JsonLink(it) }
        file.writeText( Reflector.toJson(dbLinks, pretty = true))
    }

    fun deleteAll() {
        db.withTransaction {
            db.runQuery(
                QueryDsl.delete(Meta.link).all()
            )
            Context.config.historyKeyword.clear()
            links.clear()
        }
    }

    fun delete(link: Link) {
        db.withTransaction {
            db.runQuery(QueryDsl.delete(Meta.link).single(link))
            Context.config.historyKeyword.remove(link.title ?: "")
            links.remove(link)
        }
    }

    fun openImportPicker(): Path? =
        filePicker("msg.file.import","*.sl","msg.file.import.description")

    fun openExportPicker(): Path? =
        filePicker("msg.file.export","*.sl","msg.file.import.description", FileChooserMode.Save)

    fun openIconPicker(): Path? =
        filePicker("msg.file.icon","*.*","msg.file.icon.description")

    fun openExecutorPicker(): Path? =
        filePicker("msg.file.add","*.*","msg.file.add.description")

    private fun filePicker(title: String, extension: String, description: String, mode: FileChooserMode = FileChooserMode.Single): Path? {
        return Dialog.filePicker(
            title = title.message(),
            extension = extension,
            description = description.message(),
            initialDirectory = Context.config.filePickerInitialDirectory?.toPath(),
            mode = mode,
            owner = Context.main.primaryStage
        ).firstOrNull().also {
            if( it != null )
                Context.config.filePickerInitialDirectory = it.directory.pathString
        }
    }

    fun openFolder(link: Link) {
        link.toPath()?.directory?.let {
            if( it.notExists() ) {
                Dialog.error("msg.error.no.directory".message().format(it) )
            } else {
                Desktop.open(it.toFile())
            }
        }
    }

    fun copyFolder(link: Link) {
        val path = link.toPath()?.directory ?: link.path
        Desktop.clipboard.set(path.toString())
    }

}