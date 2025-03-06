package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out PatternInstanceOfTree>.expression() = func(PatternInstanceOfTree::expression)
fun  OptionalBuilder<out PatternInstanceOfTree>.expression() = func(PatternInstanceOfTree::expression)
fun  ManyBuilder<out PatternInstanceOfTree>.expression() = func(PatternInstanceOfTree::expression)

fun  SingleBuilder<out PatternInstanceOfTree>.instanceofKeyword() = func(PatternInstanceOfTree::instanceofKeyword)
fun  OptionalBuilder<out PatternInstanceOfTree>.instanceofKeyword() = func(PatternInstanceOfTree::instanceofKeyword)
fun  ManyBuilder<out PatternInstanceOfTree>.instanceofKeyword() = func(PatternInstanceOfTree::instanceofKeyword)

fun  SingleBuilder<out PatternInstanceOfTree>.pattern() = func(PatternInstanceOfTree::pattern)
fun  OptionalBuilder<out PatternInstanceOfTree>.pattern() = func(PatternInstanceOfTree::pattern)
fun  ManyBuilder<out PatternInstanceOfTree>.pattern() = func(PatternInstanceOfTree::pattern)
