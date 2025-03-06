package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out Tree>.firstToken() = optFunc(Tree::firstToken)
fun  OptionalBuilder<out Tree>.firstToken() = optFunc(Tree::firstToken)
fun  ManyBuilder<out Tree>.firstToken() = optFunc(Tree::firstToken)

fun  SingleBuilder<out Tree>.kind() = func(Tree::kind)
fun  OptionalBuilder<out Tree>.kind() = func(Tree::kind)
fun  ManyBuilder<out Tree>.kind() = func(Tree::kind)

fun  SingleBuilder<out Tree>.lastToken() = optFunc(Tree::lastToken)
fun  OptionalBuilder<out Tree>.lastToken() = optFunc(Tree::lastToken)
fun  ManyBuilder<out Tree>.lastToken() = optFunc(Tree::lastToken)

fun  SingleBuilder<out Tree>.parent() = optFunc(Tree::parent)
fun  OptionalBuilder<out Tree>.parent() = optFunc(Tree::parent)
fun  ManyBuilder<out Tree>.parent() = optFunc(Tree::parent)
