package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.SwitchTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out SwitchTree>.closeBraceToken() = func(SwitchTree::closeBraceToken)
fun  OptionalBuilder<out SwitchTree>.closeBraceToken() = func(SwitchTree::closeBraceToken)
fun  ManyBuilder<out SwitchTree>.closeBraceToken() = func(SwitchTree::closeBraceToken)

fun  SingleBuilder<out SwitchTree>.closeParenToken() = func(SwitchTree::closeParenToken)
fun  OptionalBuilder<out SwitchTree>.closeParenToken() = func(SwitchTree::closeParenToken)
fun  ManyBuilder<out SwitchTree>.closeParenToken() = func(SwitchTree::closeParenToken)

fun  SingleBuilder<out SwitchTree>.expression() = func(SwitchTree::expression)
fun  OptionalBuilder<out SwitchTree>.expression() = func(SwitchTree::expression)
fun  ManyBuilder<out SwitchTree>.expression() = func(SwitchTree::expression)

fun  SingleBuilder<out SwitchTree>.listCases() = func(SwitchTree::cases)
fun  OptionalBuilder<out SwitchTree>.listCases() = func(SwitchTree::cases)
fun  ManyBuilder<out SwitchTree>.listCases() = func(SwitchTree::cases)
fun  SingleBuilder<out SwitchTree>.cases() = listFunc(SwitchTree::cases)
fun  OptionalBuilder<out SwitchTree>.cases() = listFunc(SwitchTree::cases)
fun  ManyBuilder<out SwitchTree>.cases() = listFunc(SwitchTree::cases)

fun  SingleBuilder<out SwitchTree>.openBraceToken() = func(SwitchTree::openBraceToken)
fun  OptionalBuilder<out SwitchTree>.openBraceToken() = func(SwitchTree::openBraceToken)
fun  ManyBuilder<out SwitchTree>.openBraceToken() = func(SwitchTree::openBraceToken)

fun  SingleBuilder<out SwitchTree>.openParenToken() = func(SwitchTree::openParenToken)
fun  OptionalBuilder<out SwitchTree>.openParenToken() = func(SwitchTree::openParenToken)
fun  ManyBuilder<out SwitchTree>.openParenToken() = func(SwitchTree::openParenToken)

fun  SingleBuilder<out SwitchTree>.switchKeyword() = func(SwitchTree::switchKeyword)
fun  OptionalBuilder<out SwitchTree>.switchKeyword() = func(SwitchTree::switchKeyword)
fun  ManyBuilder<out SwitchTree>.switchKeyword() = func(SwitchTree::switchKeyword)
