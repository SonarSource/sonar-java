package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.GuardedPatternTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out GuardedPatternTree>.expression() = func(GuardedPatternTree::expression)
fun  OptionalBuilder<out GuardedPatternTree>.expression() = func(GuardedPatternTree::expression)
fun  ManyBuilder<out GuardedPatternTree>.expression() = func(GuardedPatternTree::expression)

fun  SingleBuilder<out GuardedPatternTree>.pattern() = func(GuardedPatternTree::pattern)
fun  OptionalBuilder<out GuardedPatternTree>.pattern() = func(GuardedPatternTree::pattern)
fun  ManyBuilder<out GuardedPatternTree>.pattern() = func(GuardedPatternTree::pattern)

fun  SingleBuilder<out GuardedPatternTree>.whenOperator() = func(GuardedPatternTree::whenOperator)
fun  OptionalBuilder<out GuardedPatternTree>.whenOperator() = func(GuardedPatternTree::whenOperator)
fun  ManyBuilder<out GuardedPatternTree>.whenOperator() = func(GuardedPatternTree::whenOperator)
