package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.BlockTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out BlockTree>.closeBraceToken() = func(BlockTree::closeBraceToken)
fun  OptionalBuilder<out BlockTree>.closeBraceToken() = func(BlockTree::closeBraceToken)
fun  ManyBuilder<out BlockTree>.closeBraceToken() = func(BlockTree::closeBraceToken)

fun  SingleBuilder<out BlockTree>.listBody() = func(BlockTree::body)
fun  OptionalBuilder<out BlockTree>.listBody() = func(BlockTree::body)
fun  ManyBuilder<out BlockTree>.listBody() = func(BlockTree::body)
fun  SingleBuilder<out BlockTree>.body() = listFunc(BlockTree::body)
fun  OptionalBuilder<out BlockTree>.body() = listFunc(BlockTree::body)
fun  ManyBuilder<out BlockTree>.body() = listFunc(BlockTree::body)

fun  SingleBuilder<out BlockTree>.openBraceToken() = func(BlockTree::openBraceToken)
fun  OptionalBuilder<out BlockTree>.openBraceToken() = func(BlockTree::openBraceToken)
fun  ManyBuilder<out BlockTree>.openBraceToken() = func(BlockTree::openBraceToken)
