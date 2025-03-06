package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.IdentifierTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out IdentifierTree>.identifierToken() = func(IdentifierTree::identifierToken)
fun  OptionalBuilder<out IdentifierTree>.identifierToken() = func(IdentifierTree::identifierToken)
fun  ManyBuilder<out IdentifierTree>.identifierToken() = func(IdentifierTree::identifierToken)

fun  SingleBuilder<out IdentifierTree>.isUnnamedVariable() = func(IdentifierTree::isUnnamedVariable)
fun  OptionalBuilder<out IdentifierTree>.isUnnamedVariable() = func(IdentifierTree::isUnnamedVariable)
fun  ManyBuilder<out IdentifierTree>.isUnnamedVariable() = func(IdentifierTree::isUnnamedVariable)

fun  SingleBuilder<out IdentifierTree>.name() = func(IdentifierTree::name)
fun  OptionalBuilder<out IdentifierTree>.name() = func(IdentifierTree::name)
fun  ManyBuilder<out IdentifierTree>.name() = func(IdentifierTree::name)

fun  SingleBuilder<out IdentifierTree>.symbol() = func(IdentifierTree::symbol)
fun  OptionalBuilder<out IdentifierTree>.symbol() = func(IdentifierTree::symbol)
fun  ManyBuilder<out IdentifierTree>.symbol() = func(IdentifierTree::symbol)
