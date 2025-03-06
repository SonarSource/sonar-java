package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.DefaultPatternTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out DefaultPatternTree>.defaultToken() = func(DefaultPatternTree::defaultToken)
fun  OptionalBuilder<out DefaultPatternTree>.defaultToken() = func(DefaultPatternTree::defaultToken)
fun  ManyBuilder<out DefaultPatternTree>.defaultToken() = func(DefaultPatternTree::defaultToken)
