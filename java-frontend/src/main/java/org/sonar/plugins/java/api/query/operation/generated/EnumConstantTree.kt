package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.EnumConstantTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out EnumConstantTree>.initializer() = func(EnumConstantTree::initializer)
fun  OptionalBuilder<out EnumConstantTree>.initializer() = func(EnumConstantTree::initializer)
fun  ManyBuilder<out EnumConstantTree>.initializer() = func(EnumConstantTree::initializer)

fun  SingleBuilder<out EnumConstantTree>.listModifiers() = func(EnumConstantTree::modifiers)
fun  OptionalBuilder<out EnumConstantTree>.listModifiers() = func(EnumConstantTree::modifiers)
fun  ManyBuilder<out EnumConstantTree>.listModifiers() = func(EnumConstantTree::modifiers)
fun  SingleBuilder<out EnumConstantTree>.modifiers() = listFunc(EnumConstantTree::modifiers)
fun  OptionalBuilder<out EnumConstantTree>.modifiers() = listFunc(EnumConstantTree::modifiers)
fun  ManyBuilder<out EnumConstantTree>.modifiers() = listFunc(EnumConstantTree::modifiers)

fun  SingleBuilder<out EnumConstantTree>.separatorToken() = optFunc(EnumConstantTree::separatorToken)
fun  OptionalBuilder<out EnumConstantTree>.separatorToken() = optFunc(EnumConstantTree::separatorToken)
fun  ManyBuilder<out EnumConstantTree>.separatorToken() = optFunc(EnumConstantTree::separatorToken)

fun  SingleBuilder<out EnumConstantTree>.simpleName() = func(EnumConstantTree::simpleName)
fun  OptionalBuilder<out EnumConstantTree>.simpleName() = func(EnumConstantTree::simpleName)
fun  ManyBuilder<out EnumConstantTree>.simpleName() = func(EnumConstantTree::simpleName)
