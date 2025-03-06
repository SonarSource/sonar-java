package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ArrayTypeTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ArrayTypeTree>.closeBracketToken() = optFunc(ArrayTypeTree::closeBracketToken)
fun  OptionalBuilder<out ArrayTypeTree>.closeBracketToken() = optFunc(ArrayTypeTree::closeBracketToken)
fun  ManyBuilder<out ArrayTypeTree>.closeBracketToken() = optFunc(ArrayTypeTree::closeBracketToken)

fun  SingleBuilder<out ArrayTypeTree>.ellipsisToken() = optFunc(ArrayTypeTree::ellipsisToken)
fun  OptionalBuilder<out ArrayTypeTree>.ellipsisToken() = optFunc(ArrayTypeTree::ellipsisToken)
fun  ManyBuilder<out ArrayTypeTree>.ellipsisToken() = optFunc(ArrayTypeTree::ellipsisToken)

fun  SingleBuilder<out ArrayTypeTree>.openBracketToken() = optFunc(ArrayTypeTree::openBracketToken)
fun  OptionalBuilder<out ArrayTypeTree>.openBracketToken() = optFunc(ArrayTypeTree::openBracketToken)
fun  ManyBuilder<out ArrayTypeTree>.openBracketToken() = optFunc(ArrayTypeTree::openBracketToken)

fun  SingleBuilder<out ArrayTypeTree>.type() = func(ArrayTypeTree::type)
fun  OptionalBuilder<out ArrayTypeTree>.type() = func(ArrayTypeTree::type)
fun  ManyBuilder<out ArrayTypeTree>.type() = func(ArrayTypeTree::type)
