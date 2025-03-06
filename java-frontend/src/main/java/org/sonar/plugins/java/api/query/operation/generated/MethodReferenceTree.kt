package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.MethodReferenceTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.optListFunc

fun  SingleBuilder<out MethodReferenceTree>.doubleColon() = func(MethodReferenceTree::doubleColon)
fun  OptionalBuilder<out MethodReferenceTree>.doubleColon() = func(MethodReferenceTree::doubleColon)
fun  ManyBuilder<out MethodReferenceTree>.doubleColon() = func(MethodReferenceTree::doubleColon)

fun  SingleBuilder<out MethodReferenceTree>.expression() = func(MethodReferenceTree::expression)
fun  OptionalBuilder<out MethodReferenceTree>.expression() = func(MethodReferenceTree::expression)
fun  ManyBuilder<out MethodReferenceTree>.expression() = func(MethodReferenceTree::expression)

fun  SingleBuilder<out MethodReferenceTree>.listTypeArguments() = optFunc(MethodReferenceTree::typeArguments)
fun  OptionalBuilder<out MethodReferenceTree>.listTypeArguments() = optFunc(MethodReferenceTree::typeArguments)
fun  ManyBuilder<out MethodReferenceTree>.listTypeArguments() = optFunc(MethodReferenceTree::typeArguments)
fun  SingleBuilder<out MethodReferenceTree>.typeArguments() = optListFunc(MethodReferenceTree::typeArguments)
fun  OptionalBuilder<out MethodReferenceTree>.typeArguments() = optListFunc(MethodReferenceTree::typeArguments)
fun  ManyBuilder<out MethodReferenceTree>.typeArguments() = optListFunc(MethodReferenceTree::typeArguments)

fun  SingleBuilder<out MethodReferenceTree>.method() = func(MethodReferenceTree::method)
fun  OptionalBuilder<out MethodReferenceTree>.method() = func(MethodReferenceTree::method)
fun  ManyBuilder<out MethodReferenceTree>.method() = func(MethodReferenceTree::method)
