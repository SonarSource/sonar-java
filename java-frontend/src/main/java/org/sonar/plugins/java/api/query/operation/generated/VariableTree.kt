package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.VariableTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out VariableTree>.endToken() = optFunc(VariableTree::endToken)
fun  OptionalBuilder<out VariableTree>.endToken() = optFunc(VariableTree::endToken)
fun  ManyBuilder<out VariableTree>.endToken() = optFunc(VariableTree::endToken)

fun  SingleBuilder<out VariableTree>.equalToken() = optFunc(VariableTree::equalToken)
fun  OptionalBuilder<out VariableTree>.equalToken() = optFunc(VariableTree::equalToken)
fun  ManyBuilder<out VariableTree>.equalToken() = optFunc(VariableTree::equalToken)

fun  SingleBuilder<out VariableTree>.initializer() = optFunc(VariableTree::initializer)
fun  OptionalBuilder<out VariableTree>.initializer() = optFunc(VariableTree::initializer)
fun  ManyBuilder<out VariableTree>.initializer() = optFunc(VariableTree::initializer)

fun  SingleBuilder<out VariableTree>.listModifiers() = func(VariableTree::modifiers)
fun  OptionalBuilder<out VariableTree>.listModifiers() = func(VariableTree::modifiers)
fun  ManyBuilder<out VariableTree>.listModifiers() = func(VariableTree::modifiers)
fun  SingleBuilder<out VariableTree>.modifiers() = listFunc(VariableTree::modifiers)
fun  OptionalBuilder<out VariableTree>.modifiers() = listFunc(VariableTree::modifiers)
fun  ManyBuilder<out VariableTree>.modifiers() = listFunc(VariableTree::modifiers)

fun  SingleBuilder<out VariableTree>.simpleName() = func(VariableTree::simpleName)
fun  OptionalBuilder<out VariableTree>.simpleName() = func(VariableTree::simpleName)
fun  ManyBuilder<out VariableTree>.simpleName() = func(VariableTree::simpleName)

fun  SingleBuilder<out VariableTree>.symbol() = func(VariableTree::symbol)
fun  OptionalBuilder<out VariableTree>.symbol() = func(VariableTree::symbol)
fun  ManyBuilder<out VariableTree>.symbol() = func(VariableTree::symbol)

fun  SingleBuilder<out VariableTree>.type() = func(VariableTree::type)
fun  OptionalBuilder<out VariableTree>.type() = func(VariableTree::type)
fun  ManyBuilder<out VariableTree>.type() = func(VariableTree::type)
