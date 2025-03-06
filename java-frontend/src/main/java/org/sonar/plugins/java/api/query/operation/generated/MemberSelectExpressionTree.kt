package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out MemberSelectExpressionTree>.expression() = func(MemberSelectExpressionTree::expression)
fun  OptionalBuilder<out MemberSelectExpressionTree>.expression() = func(MemberSelectExpressionTree::expression)
fun  ManyBuilder<out MemberSelectExpressionTree>.expression() = func(MemberSelectExpressionTree::expression)

fun  SingleBuilder<out MemberSelectExpressionTree>.identifier() = func(MemberSelectExpressionTree::identifier)
fun  OptionalBuilder<out MemberSelectExpressionTree>.identifier() = func(MemberSelectExpressionTree::identifier)
fun  ManyBuilder<out MemberSelectExpressionTree>.identifier() = func(MemberSelectExpressionTree::identifier)

fun  SingleBuilder<out MemberSelectExpressionTree>.operatorToken() = func(MemberSelectExpressionTree::operatorToken)
fun  OptionalBuilder<out MemberSelectExpressionTree>.operatorToken() = func(MemberSelectExpressionTree::operatorToken)
fun  ManyBuilder<out MemberSelectExpressionTree>.operatorToken() = func(MemberSelectExpressionTree::operatorToken)
