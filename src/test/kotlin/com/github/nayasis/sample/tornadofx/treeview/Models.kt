package com.github.nayasis.sample.tornadofx.treeview

data class Group(val name: String, val children: List<Group>? = null )

sealed class PersonTreeItem(open val name: String)

object TreeRoot: PersonTreeItem("Departments")

data class Department(override val name: String): PersonTreeItem(name)
data class Person(override val name: String, val department: String): PersonTreeItem(name)

val group = Group("Parent", listOf(
    Group("Child1"),
    Group("Child2"),
    Group("Child3", listOf(
        Group("Grand child 3.1", listOf(
            Group("Great grandchild 3.1.1"),
            Group("Great grandchild 3.1.2"),
        ))
    ))
))

val persons = listOf(
    Person("Mary Hanes", "Marketing"),
    Person("Steve Folley", "Customer Service"),
    Person("John Ramsy", "IT Help Desk"),
    Person("Erlick Foyes", "Customer Service"),
    Person("Erin James", "Marketing"),
    Person("Jacob Mays", "IT Help Desk"),
    Person("Larry Cable", "Customer Service"),
)