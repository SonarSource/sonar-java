package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TypeCastTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out TypeCastTree>.andToken() = optFunc(TypeCastTree::andToken)
fun  OptionalBuilder<out TypeCastTree>.andToken() = optFunc(TypeCastTree::andToken)
fun  ManyBuilder<out TypeCastTree>.andToken() = optFunc(TypeCastTree::andToken)

fun  SingleBuilder<out TypeCastTree>.closeParenToken() = func(TypeCastTree::closeParenToken)
fun  OptionalBuilder<out TypeCastTree>.closeParenToken() = func(TypeCastTree::closeParenToken)
fun  ManyBuilder<out TypeCastTree>.closeParenToken() = func(TypeCastTree::closeParenToken)

fun  SingleBuilder<out TypeCastTree>.expression() = func(TypeCastTree::expression)
fun  OptionalBuilder<out TypeCastTree>.expression() = func(TypeCastTree::expression)
fun  ManyBuilder<out TypeCastTree>.expression() = func(TypeCastTree::expression)

fun  SingleBuilder<out TypeCastTree>.listBounds() = func(TypeCastTree::bounds)
fun  OptionalBuilder<out TypeCastTree>.listBounds() = func(TypeCastTree::bounds)
fun  ManyBuilder<out TypeCastTree>.listBounds() = func(TypeCastTree::bounds)
fun  SingleBuilder<out TypeCastTree>.bounds() = listFunc(TypeCastTree::bounds)
fun  OptionalBuilder<out TypeCastTree>.bounds() = listFunc(TypeCastTree::bounds)
fun  ManyBuilder<out TypeCastTree>.bounds() = listFunc(TypeCastTree::bounds)

fun  SingleBuilder<out TypeCastTree>.openParenToken() = func(TypeCastTree::openParenToken)
fun  OptionalBuilder<out TypeCastTree>.openParenToken() = func(TypeCastTree::openParenToken)
fun  ManyBuilder<out TypeCastTree>.openParenToken() = func(TypeCastTree::openParenToken)

fun  SingleBuilder<out TypeCastTree>.type() = func(TypeCastTree::type)
fun  OptionalBuilder<out TypeCastTree>.type() = func(TypeCastTree::type)
fun  ManyBuilder<out TypeCastTree>.type() = func(TypeCastTree::type)
