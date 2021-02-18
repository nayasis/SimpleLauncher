package com.github.nayasis.sample.itemviewmodel.pojo.controller

import com.github.nayasis.sample.itemviewmodel.pojo.model.Category
import com.github.nayasis.sample.itemviewmodel.pojo.model.CategoryModel
import com.github.nayasis.sample.itemviewmodel.pojo.model.Entry
import com.github.nayasis.sample.itemviewmodel.pojo.model.EntryModel
import tornadofx.Controller

class MainController : Controller() {

    val categoryModel = CategoryModel()
    val entryModel = EntryModel()

    val categories = listOf(
            Category("a", 0),
            Category("b", 1),
            Category("c", 2),
            Category("d", 3)
    )

    val entries = listOf(
            listOf(
                    Entry("a", "aaa", "a writer"),
                    Entry("a1", "aaa1", "another writer"),
                    Entry("a2", "aaa2", "an amateur writer"),
                    Entry("a3", "aaa3", "a screen writer"),
                    Entry("a4", "aaa4", "the writer")),
            listOf(
                    Entry("b", "bbb", "a writer")),
            listOf(
                    Entry("c", "ccc", "a writer")),
            listOf(
                    Entry("d", "ddd", "a writer"))
    )

}