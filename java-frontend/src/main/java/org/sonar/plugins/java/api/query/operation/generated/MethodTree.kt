package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.MethodTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out MethodTree>.block() = optFunc(MethodTree::block)
fun  OptionalBuilder<out MethodTree>.block() = optFunc(MethodTree::block)
fun  ManyBuilder<out MethodTree>.block() = optFunc(MethodTree::block)

fun  SingleBuilder<out MethodTree>.cfg() = optFunc(MethodTree::cfg)
fun  OptionalBuilder<out MethodTree>.cfg() = optFunc(MethodTree::cfg)
fun  ManyBuilder<out MethodTree>.cfg() = optFunc(MethodTree::cfg)

fun  SingleBuilder<out MethodTree>.closeParenToken() = optFunc(MethodTree::closeParenToken)
fun  OptionalBuilder<out MethodTree>.closeParenToken() = optFunc(MethodTree::closeParenToken)
fun  ManyBuilder<out MethodTree>.closeParenToken() = optFunc(MethodTree::closeParenToken)

fun  SingleBuilder<out MethodTree>.defaultToken() = optFunc(MethodTree::defaultToken)
fun  OptionalBuilder<out MethodTree>.defaultToken() = optFunc(MethodTree::defaultToken)
fun  ManyBuilder<out MethodTree>.defaultToken() = optFunc(MethodTree::defaultToken)

fun  SingleBuilder<out MethodTree>.defaultValue() = optFunc(MethodTree::defaultValue)
fun  OptionalBuilder<out MethodTree>.defaultValue() = optFunc(MethodTree::defaultValue)
fun  ManyBuilder<out MethodTree>.defaultValue() = optFunc(MethodTree::defaultValue)

fun  SingleBuilder<out MethodTree>.isOverriding() = optFunc(MethodTree::isOverriding)
fun  OptionalBuilder<out MethodTree>.isOverriding() = optFunc(MethodTree::isOverriding)
fun  ManyBuilder<out MethodTree>.isOverriding() = optFunc(MethodTree::isOverriding)

fun  SingleBuilder<out MethodTree>.listModifiers() = func(MethodTree::modifiers)
fun  OptionalBuilder<out MethodTree>.listModifiers() = func(MethodTree::modifiers)
fun  ManyBuilder<out MethodTree>.listModifiers() = func(MethodTree::modifiers)
fun  SingleBuilder<out MethodTree>.modifiers() = listFunc(MethodTree::modifiers)
fun  OptionalBuilder<out MethodTree>.modifiers() = listFunc(MethodTree::modifiers)
fun  ManyBuilder<out MethodTree>.modifiers() = listFunc(MethodTree::modifiers)

fun  SingleBuilder<out MethodTree>.listParameters() = func(MethodTree::parameters)
fun  OptionalBuilder<out MethodTree>.listParameters() = func(MethodTree::parameters)
fun  ManyBuilder<out MethodTree>.listParameters() = func(MethodTree::parameters)
fun  SingleBuilder<out MethodTree>.parameters() = listFunc(MethodTree::parameters)
fun  OptionalBuilder<out MethodTree>.parameters() = listFunc(MethodTree::parameters)
fun  ManyBuilder<out MethodTree>.parameters() = listFunc(MethodTree::parameters)

fun  SingleBuilder<out MethodTree>.listThrowsClauses() = func(MethodTree::throwsClauses)
fun  OptionalBuilder<out MethodTree>.listThrowsClauses() = func(MethodTree::throwsClauses)
fun  ManyBuilder<out MethodTree>.listThrowsClauses() = func(MethodTree::throwsClauses)
fun  SingleBuilder<out MethodTree>.throwsClauses() = listFunc(MethodTree::throwsClauses)
fun  OptionalBuilder<out MethodTree>.throwsClauses() = listFunc(MethodTree::throwsClauses)
fun  ManyBuilder<out MethodTree>.throwsClauses() = listFunc(MethodTree::throwsClauses)

fun  SingleBuilder<out MethodTree>.listTypeParameters() = func(MethodTree::typeParameters)
fun  OptionalBuilder<out MethodTree>.listTypeParameters() = func(MethodTree::typeParameters)
fun  ManyBuilder<out MethodTree>.listTypeParameters() = func(MethodTree::typeParameters)
fun  SingleBuilder<out MethodTree>.typeParameters() = listFunc(MethodTree::typeParameters)
fun  OptionalBuilder<out MethodTree>.typeParameters() = listFunc(MethodTree::typeParameters)
fun  ManyBuilder<out MethodTree>.typeParameters() = listFunc(MethodTree::typeParameters)

fun  SingleBuilder<out MethodTree>.openParenToken() = optFunc(MethodTree::openParenToken)
fun  OptionalBuilder<out MethodTree>.openParenToken() = optFunc(MethodTree::openParenToken)
fun  ManyBuilder<out MethodTree>.openParenToken() = optFunc(MethodTree::openParenToken)

fun  SingleBuilder<out MethodTree>.returnType() = optFunc(MethodTree::returnType)
fun  OptionalBuilder<out MethodTree>.returnType() = optFunc(MethodTree::returnType)
fun  ManyBuilder<out MethodTree>.returnType() = optFunc(MethodTree::returnType)

fun  SingleBuilder<out MethodTree>.semicolonToken() = optFunc(MethodTree::semicolonToken)
fun  OptionalBuilder<out MethodTree>.semicolonToken() = optFunc(MethodTree::semicolonToken)
fun  ManyBuilder<out MethodTree>.semicolonToken() = optFunc(MethodTree::semicolonToken)

fun  SingleBuilder<out MethodTree>.simpleName() = func(MethodTree::simpleName)
fun  OptionalBuilder<out MethodTree>.simpleName() = func(MethodTree::simpleName)
fun  ManyBuilder<out MethodTree>.simpleName() = func(MethodTree::simpleName)

fun  SingleBuilder<out MethodTree>.symbol() = func(MethodTree::symbol)
fun  OptionalBuilder<out MethodTree>.symbol() = func(MethodTree::symbol)
fun  ManyBuilder<out MethodTree>.symbol() = func(MethodTree::symbol)

fun  SingleBuilder<out MethodTree>.throwsToken() = func(MethodTree::throwsToken)
fun  OptionalBuilder<out MethodTree>.throwsToken() = func(MethodTree::throwsToken)
fun  ManyBuilder<out MethodTree>.throwsToken() = func(MethodTree::throwsToken)
