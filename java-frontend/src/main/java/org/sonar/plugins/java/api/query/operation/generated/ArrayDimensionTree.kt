package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ArrayDimensionTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out ArrayDimensionTree>.closeBracketToken() = func(ArrayDimensionTree::closeBracketToken)
fun  OptionalBuilder<out ArrayDimensionTree>.closeBracketToken() = func(ArrayDimensionTree::closeBracketToken)
fun  ManyBuilder<out ArrayDimensionTree>.closeBracketToken() = func(ArrayDimensionTree::closeBracketToken)

fun  SingleBuilder<out ArrayDimensionTree>.expression() = optFunc(ArrayDimensionTree::expression)
fun  OptionalBuilder<out ArrayDimensionTree>.expression() = optFunc(ArrayDimensionTree::expression)
fun  ManyBuilder<out ArrayDimensionTree>.expression() = optFunc(ArrayDimensionTree::expression)

fun  SingleBuilder<out ArrayDimensionTree>.listAnnotations() = func(ArrayDimensionTree::annotations)
fun  OptionalBuilder<out ArrayDimensionTree>.listAnnotations() = func(ArrayDimensionTree::annotations)
fun  ManyBuilder<out ArrayDimensionTree>.listAnnotations() = func(ArrayDimensionTree::annotations)
fun  SingleBuilder<out ArrayDimensionTree>.annotations() = listFunc(ArrayDimensionTree::annotations)
fun  OptionalBuilder<out ArrayDimensionTree>.annotations() = listFunc(ArrayDimensionTree::annotations)
fun  ManyBuilder<out ArrayDimensionTree>.annotations() = listFunc(ArrayDimensionTree::annotations)

fun  SingleBuilder<out ArrayDimensionTree>.openBracketToken() = func(ArrayDimensionTree::openBracketToken)
fun  OptionalBuilder<out ArrayDimensionTree>.openBracketToken() = func(ArrayDimensionTree::openBracketToken)
fun  ManyBuilder<out ArrayDimensionTree>.openBracketToken() = func(ArrayDimensionTree::openBracketToken)
