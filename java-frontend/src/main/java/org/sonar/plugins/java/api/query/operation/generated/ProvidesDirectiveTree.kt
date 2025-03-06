package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out ProvidesDirectiveTree>.listTypeNames() = func(ProvidesDirectiveTree::typeNames)
fun  OptionalBuilder<out ProvidesDirectiveTree>.listTypeNames() = func(ProvidesDirectiveTree::typeNames)
fun  ManyBuilder<out ProvidesDirectiveTree>.listTypeNames() = func(ProvidesDirectiveTree::typeNames)
fun  SingleBuilder<out ProvidesDirectiveTree>.typeNames() = listFunc(ProvidesDirectiveTree::typeNames)
fun  OptionalBuilder<out ProvidesDirectiveTree>.typeNames() = listFunc(ProvidesDirectiveTree::typeNames)
fun  ManyBuilder<out ProvidesDirectiveTree>.typeNames() = listFunc(ProvidesDirectiveTree::typeNames)

fun  SingleBuilder<out ProvidesDirectiveTree>.typeName() = func(ProvidesDirectiveTree::typeName)
fun  OptionalBuilder<out ProvidesDirectiveTree>.typeName() = func(ProvidesDirectiveTree::typeName)
fun  ManyBuilder<out ProvidesDirectiveTree>.typeName() = func(ProvidesDirectiveTree::typeName)

fun  SingleBuilder<out ProvidesDirectiveTree>.withKeyword() = func(ProvidesDirectiveTree::withKeyword)
fun  OptionalBuilder<out ProvidesDirectiveTree>.withKeyword() = func(ProvidesDirectiveTree::withKeyword)
fun  ManyBuilder<out ProvidesDirectiveTree>.withKeyword() = func(ProvidesDirectiveTree::withKeyword)
