package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ForStatementTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out ForStatementTree>.closeParenToken() = func(ForStatementTree::closeParenToken)
fun  OptionalBuilder<out ForStatementTree>.closeParenToken() = func(ForStatementTree::closeParenToken)
fun  ManyBuilder<out ForStatementTree>.closeParenToken() = func(ForStatementTree::closeParenToken)

fun  SingleBuilder<out ForStatementTree>.condition() = optFunc(ForStatementTree::condition)
fun  OptionalBuilder<out ForStatementTree>.condition() = optFunc(ForStatementTree::condition)
fun  ManyBuilder<out ForStatementTree>.condition() = optFunc(ForStatementTree::condition)

fun  SingleBuilder<out ForStatementTree>.firstSemicolonToken() = func(ForStatementTree::firstSemicolonToken)
fun  OptionalBuilder<out ForStatementTree>.firstSemicolonToken() = func(ForStatementTree::firstSemicolonToken)
fun  ManyBuilder<out ForStatementTree>.firstSemicolonToken() = func(ForStatementTree::firstSemicolonToken)

fun  SingleBuilder<out ForStatementTree>.forKeyword() = func(ForStatementTree::forKeyword)
fun  OptionalBuilder<out ForStatementTree>.forKeyword() = func(ForStatementTree::forKeyword)
fun  ManyBuilder<out ForStatementTree>.forKeyword() = func(ForStatementTree::forKeyword)

fun  SingleBuilder<out ForStatementTree>.listInitializer() = func(ForStatementTree::initializer)
fun  OptionalBuilder<out ForStatementTree>.listInitializer() = func(ForStatementTree::initializer)
fun  ManyBuilder<out ForStatementTree>.listInitializer() = func(ForStatementTree::initializer)
fun  SingleBuilder<out ForStatementTree>.initializer() = listFunc(ForStatementTree::initializer)
fun  OptionalBuilder<out ForStatementTree>.initializer() = listFunc(ForStatementTree::initializer)
fun  ManyBuilder<out ForStatementTree>.initializer() = listFunc(ForStatementTree::initializer)

fun  SingleBuilder<out ForStatementTree>.listUpdate() = func(ForStatementTree::update)
fun  OptionalBuilder<out ForStatementTree>.listUpdate() = func(ForStatementTree::update)
fun  ManyBuilder<out ForStatementTree>.listUpdate() = func(ForStatementTree::update)
fun  SingleBuilder<out ForStatementTree>.update() = listFunc(ForStatementTree::update)
fun  OptionalBuilder<out ForStatementTree>.update() = listFunc(ForStatementTree::update)
fun  ManyBuilder<out ForStatementTree>.update() = listFunc(ForStatementTree::update)

fun  SingleBuilder<out ForStatementTree>.openParenToken() = func(ForStatementTree::openParenToken)
fun  OptionalBuilder<out ForStatementTree>.openParenToken() = func(ForStatementTree::openParenToken)
fun  ManyBuilder<out ForStatementTree>.openParenToken() = func(ForStatementTree::openParenToken)

fun  SingleBuilder<out ForStatementTree>.secondSemicolonToken() = func(ForStatementTree::secondSemicolonToken)
fun  OptionalBuilder<out ForStatementTree>.secondSemicolonToken() = func(ForStatementTree::secondSemicolonToken)
fun  ManyBuilder<out ForStatementTree>.secondSemicolonToken() = func(ForStatementTree::secondSemicolonToken)

fun  SingleBuilder<out ForStatementTree>.statement() = func(ForStatementTree::statement)
fun  OptionalBuilder<out ForStatementTree>.statement() = func(ForStatementTree::statement)
fun  ManyBuilder<out ForStatementTree>.statement() = func(ForStatementTree::statement)
