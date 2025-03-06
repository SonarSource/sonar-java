package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ForEachStatement
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ForEachStatement>.closeParenToken() = func(ForEachStatement::closeParenToken)
fun  OptionalBuilder<out ForEachStatement>.closeParenToken() = func(ForEachStatement::closeParenToken)
fun  ManyBuilder<out ForEachStatement>.closeParenToken() = func(ForEachStatement::closeParenToken)

fun  SingleBuilder<out ForEachStatement>.colonToken() = func(ForEachStatement::colonToken)
fun  OptionalBuilder<out ForEachStatement>.colonToken() = func(ForEachStatement::colonToken)
fun  ManyBuilder<out ForEachStatement>.colonToken() = func(ForEachStatement::colonToken)

fun  SingleBuilder<out ForEachStatement>.expression() = func(ForEachStatement::expression)
fun  OptionalBuilder<out ForEachStatement>.expression() = func(ForEachStatement::expression)
fun  ManyBuilder<out ForEachStatement>.expression() = func(ForEachStatement::expression)

fun  SingleBuilder<out ForEachStatement>.forKeyword() = func(ForEachStatement::forKeyword)
fun  OptionalBuilder<out ForEachStatement>.forKeyword() = func(ForEachStatement::forKeyword)
fun  ManyBuilder<out ForEachStatement>.forKeyword() = func(ForEachStatement::forKeyword)

fun  SingleBuilder<out ForEachStatement>.openParenToken() = func(ForEachStatement::openParenToken)
fun  OptionalBuilder<out ForEachStatement>.openParenToken() = func(ForEachStatement::openParenToken)
fun  ManyBuilder<out ForEachStatement>.openParenToken() = func(ForEachStatement::openParenToken)

fun  SingleBuilder<out ForEachStatement>.statement() = func(ForEachStatement::statement)
fun  OptionalBuilder<out ForEachStatement>.statement() = func(ForEachStatement::statement)
fun  ManyBuilder<out ForEachStatement>.statement() = func(ForEachStatement::statement)

fun  SingleBuilder<out ForEachStatement>.variable() = func(ForEachStatement::variable)
fun  OptionalBuilder<out ForEachStatement>.variable() = func(ForEachStatement::variable)
fun  ManyBuilder<out ForEachStatement>.variable() = func(ForEachStatement::variable)
