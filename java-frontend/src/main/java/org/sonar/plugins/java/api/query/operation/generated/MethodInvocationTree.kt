package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.MethodInvocationTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.optListFunc
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out MethodInvocationTree>.listArguments() = func(MethodInvocationTree::arguments)
fun  OptionalBuilder<out MethodInvocationTree>.listArguments() = func(MethodInvocationTree::arguments)
fun  ManyBuilder<out MethodInvocationTree>.listArguments() = func(MethodInvocationTree::arguments)
fun  SingleBuilder<out MethodInvocationTree>.arguments() = listFunc(MethodInvocationTree::arguments)
fun  OptionalBuilder<out MethodInvocationTree>.arguments() = listFunc(MethodInvocationTree::arguments)
fun  ManyBuilder<out MethodInvocationTree>.arguments() = listFunc(MethodInvocationTree::arguments)

fun  SingleBuilder<out MethodInvocationTree>.listTypeArguments() = optFunc(MethodInvocationTree::typeArguments)
fun  OptionalBuilder<out MethodInvocationTree>.listTypeArguments() = optFunc(MethodInvocationTree::typeArguments)
fun  ManyBuilder<out MethodInvocationTree>.listTypeArguments() = optFunc(MethodInvocationTree::typeArguments)
fun  SingleBuilder<out MethodInvocationTree>.typeArguments() = optListFunc(MethodInvocationTree::typeArguments)
fun  OptionalBuilder<out MethodInvocationTree>.typeArguments() = optListFunc(MethodInvocationTree::typeArguments)
fun  ManyBuilder<out MethodInvocationTree>.typeArguments() = optListFunc(MethodInvocationTree::typeArguments)

fun  SingleBuilder<out MethodInvocationTree>.methodSelect() = func(MethodInvocationTree::methodSelect)
fun  OptionalBuilder<out MethodInvocationTree>.methodSelect() = func(MethodInvocationTree::methodSelect)
fun  ManyBuilder<out MethodInvocationTree>.methodSelect() = func(MethodInvocationTree::methodSelect)

fun  SingleBuilder<out MethodInvocationTree>.methodSymbol() = func(MethodInvocationTree::methodSymbol)
fun  OptionalBuilder<out MethodInvocationTree>.methodSymbol() = func(MethodInvocationTree::methodSymbol)
fun  ManyBuilder<out MethodInvocationTree>.methodSymbol() = func(MethodInvocationTree::methodSymbol)
