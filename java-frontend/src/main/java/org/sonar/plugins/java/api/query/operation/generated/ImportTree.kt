package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ImportTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out ImportTree>.importKeyword() = func(ImportTree::importKeyword)
fun  OptionalBuilder<out ImportTree>.importKeyword() = func(ImportTree::importKeyword)
fun  ManyBuilder<out ImportTree>.importKeyword() = func(ImportTree::importKeyword)

fun  SingleBuilder<out ImportTree>.isStatic() = func(ImportTree::isStatic)
fun  OptionalBuilder<out ImportTree>.isStatic() = func(ImportTree::isStatic)
fun  ManyBuilder<out ImportTree>.isStatic() = func(ImportTree::isStatic)

fun  SingleBuilder<out ImportTree>.qualifiedIdentifier() = func(ImportTree::qualifiedIdentifier)
fun  OptionalBuilder<out ImportTree>.qualifiedIdentifier() = func(ImportTree::qualifiedIdentifier)
fun  ManyBuilder<out ImportTree>.qualifiedIdentifier() = func(ImportTree::qualifiedIdentifier)

fun  SingleBuilder<out ImportTree>.semicolonToken() = func(ImportTree::semicolonToken)
fun  OptionalBuilder<out ImportTree>.semicolonToken() = func(ImportTree::semicolonToken)
fun  ManyBuilder<out ImportTree>.semicolonToken() = func(ImportTree::semicolonToken)

fun  SingleBuilder<out ImportTree>.staticKeyword() = optFunc(ImportTree::staticKeyword)
fun  OptionalBuilder<out ImportTree>.staticKeyword() = optFunc(ImportTree::staticKeyword)
fun  ManyBuilder<out ImportTree>.staticKeyword() = optFunc(ImportTree::staticKeyword)

fun  SingleBuilder<out ImportTree>.symbol() = optFunc(ImportTree::symbol)
fun  OptionalBuilder<out ImportTree>.symbol() = optFunc(ImportTree::symbol)
fun  ManyBuilder<out ImportTree>.symbol() = optFunc(ImportTree::symbol)
