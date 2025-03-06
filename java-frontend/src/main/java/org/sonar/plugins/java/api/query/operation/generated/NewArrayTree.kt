package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.NewArrayTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out NewArrayTree>.closeBraceToken() = optFunc(NewArrayTree::closeBraceToken)
fun  OptionalBuilder<out NewArrayTree>.closeBraceToken() = optFunc(NewArrayTree::closeBraceToken)
fun  ManyBuilder<out NewArrayTree>.closeBraceToken() = optFunc(NewArrayTree::closeBraceToken)

fun  SingleBuilder<out NewArrayTree>.listDimensions() = func(NewArrayTree::dimensions)
fun  OptionalBuilder<out NewArrayTree>.listDimensions() = func(NewArrayTree::dimensions)
fun  ManyBuilder<out NewArrayTree>.listDimensions() = func(NewArrayTree::dimensions)
fun  SingleBuilder<out NewArrayTree>.dimensions() = listFunc(NewArrayTree::dimensions)
fun  OptionalBuilder<out NewArrayTree>.dimensions() = listFunc(NewArrayTree::dimensions)
fun  ManyBuilder<out NewArrayTree>.dimensions() = listFunc(NewArrayTree::dimensions)

fun  SingleBuilder<out NewArrayTree>.listInitializers() = func(NewArrayTree::initializers)
fun  OptionalBuilder<out NewArrayTree>.listInitializers() = func(NewArrayTree::initializers)
fun  ManyBuilder<out NewArrayTree>.listInitializers() = func(NewArrayTree::initializers)
fun  SingleBuilder<out NewArrayTree>.initializers() = listFunc(NewArrayTree::initializers)
fun  OptionalBuilder<out NewArrayTree>.initializers() = listFunc(NewArrayTree::initializers)
fun  ManyBuilder<out NewArrayTree>.initializers() = listFunc(NewArrayTree::initializers)

fun  SingleBuilder<out NewArrayTree>.newKeyword() = optFunc(NewArrayTree::newKeyword)
fun  OptionalBuilder<out NewArrayTree>.newKeyword() = optFunc(NewArrayTree::newKeyword)
fun  ManyBuilder<out NewArrayTree>.newKeyword() = optFunc(NewArrayTree::newKeyword)

fun  SingleBuilder<out NewArrayTree>.openBraceToken() = optFunc(NewArrayTree::openBraceToken)
fun  OptionalBuilder<out NewArrayTree>.openBraceToken() = optFunc(NewArrayTree::openBraceToken)
fun  ManyBuilder<out NewArrayTree>.openBraceToken() = optFunc(NewArrayTree::openBraceToken)

fun  SingleBuilder<out NewArrayTree>.type() = optFunc(NewArrayTree::type)
fun  OptionalBuilder<out NewArrayTree>.type() = optFunc(NewArrayTree::type)
fun  ManyBuilder<out NewArrayTree>.type() = optFunc(NewArrayTree::type)
