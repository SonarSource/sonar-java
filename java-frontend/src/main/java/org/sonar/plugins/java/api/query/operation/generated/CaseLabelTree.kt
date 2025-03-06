package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.CaseLabelTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out CaseLabelTree>.caseOrDefaultKeyword() = func(CaseLabelTree::caseOrDefaultKeyword)
fun  OptionalBuilder<out CaseLabelTree>.caseOrDefaultKeyword() = func(CaseLabelTree::caseOrDefaultKeyword)
fun  ManyBuilder<out CaseLabelTree>.caseOrDefaultKeyword() = func(CaseLabelTree::caseOrDefaultKeyword)

fun  SingleBuilder<out CaseLabelTree>.colonOrArrowToken() = func(CaseLabelTree::colonOrArrowToken)
fun  OptionalBuilder<out CaseLabelTree>.colonOrArrowToken() = func(CaseLabelTree::colonOrArrowToken)
fun  ManyBuilder<out CaseLabelTree>.colonOrArrowToken() = func(CaseLabelTree::colonOrArrowToken)

fun  SingleBuilder<out CaseLabelTree>.isFallThrough() = func(CaseLabelTree::isFallThrough)
fun  OptionalBuilder<out CaseLabelTree>.isFallThrough() = func(CaseLabelTree::isFallThrough)
fun  ManyBuilder<out CaseLabelTree>.isFallThrough() = func(CaseLabelTree::isFallThrough)

fun  SingleBuilder<out CaseLabelTree>.listExpressions() = func(CaseLabelTree::expressions)
fun  OptionalBuilder<out CaseLabelTree>.listExpressions() = func(CaseLabelTree::expressions)
fun  ManyBuilder<out CaseLabelTree>.listExpressions() = func(CaseLabelTree::expressions)
fun  SingleBuilder<out CaseLabelTree>.expressions() = listFunc(CaseLabelTree::expressions)
fun  OptionalBuilder<out CaseLabelTree>.expressions() = listFunc(CaseLabelTree::expressions)
fun  ManyBuilder<out CaseLabelTree>.expressions() = listFunc(CaseLabelTree::expressions)
