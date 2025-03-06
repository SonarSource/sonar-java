package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TypeParameterTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out TypeParameterTree>.extendToken() = optFunc(TypeParameterTree::extendToken)
fun  OptionalBuilder<out TypeParameterTree>.extendToken() = optFunc(TypeParameterTree::extendToken)
fun  ManyBuilder<out TypeParameterTree>.extendToken() = optFunc(TypeParameterTree::extendToken)

fun  SingleBuilder<out TypeParameterTree>.identifier() = func(TypeParameterTree::identifier)
fun  OptionalBuilder<out TypeParameterTree>.identifier() = func(TypeParameterTree::identifier)
fun  ManyBuilder<out TypeParameterTree>.identifier() = func(TypeParameterTree::identifier)

fun  SingleBuilder<out TypeParameterTree>.listBounds() = func(TypeParameterTree::bounds)
fun  OptionalBuilder<out TypeParameterTree>.listBounds() = func(TypeParameterTree::bounds)
fun  ManyBuilder<out TypeParameterTree>.listBounds() = func(TypeParameterTree::bounds)
fun  SingleBuilder<out TypeParameterTree>.bounds() = listFunc(TypeParameterTree::bounds)
fun  OptionalBuilder<out TypeParameterTree>.bounds() = listFunc(TypeParameterTree::bounds)
fun  ManyBuilder<out TypeParameterTree>.bounds() = listFunc(TypeParameterTree::bounds)

fun  SingleBuilder<out TypeParameterTree>.symbol() = func(TypeParameterTree::symbol)
fun  OptionalBuilder<out TypeParameterTree>.symbol() = func(TypeParameterTree::symbol)
fun  ManyBuilder<out TypeParameterTree>.symbol() = func(TypeParameterTree::symbol)
