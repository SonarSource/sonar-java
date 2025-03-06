package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.CatchTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out CatchTree>.block() = func(CatchTree::block)
fun  OptionalBuilder<out CatchTree>.block() = func(CatchTree::block)
fun  ManyBuilder<out CatchTree>.block() = func(CatchTree::block)

fun  SingleBuilder<out CatchTree>.catchKeyword() = func(CatchTree::catchKeyword)
fun  OptionalBuilder<out CatchTree>.catchKeyword() = func(CatchTree::catchKeyword)
fun  ManyBuilder<out CatchTree>.catchKeyword() = func(CatchTree::catchKeyword)

fun  SingleBuilder<out CatchTree>.closeParenToken() = func(CatchTree::closeParenToken)
fun  OptionalBuilder<out CatchTree>.closeParenToken() = func(CatchTree::closeParenToken)
fun  ManyBuilder<out CatchTree>.closeParenToken() = func(CatchTree::closeParenToken)

fun  SingleBuilder<out CatchTree>.openParenToken() = func(CatchTree::openParenToken)
fun  OptionalBuilder<out CatchTree>.openParenToken() = func(CatchTree::openParenToken)
fun  ManyBuilder<out CatchTree>.openParenToken() = func(CatchTree::openParenToken)

fun  SingleBuilder<out CatchTree>.parameter() = func(CatchTree::parameter)
fun  OptionalBuilder<out CatchTree>.parameter() = func(CatchTree::parameter)
fun  ManyBuilder<out CatchTree>.parameter() = func(CatchTree::parameter)
