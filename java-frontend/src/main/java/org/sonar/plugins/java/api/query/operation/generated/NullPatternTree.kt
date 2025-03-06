package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.NullPatternTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out NullPatternTree>.nullLiteral() = func(NullPatternTree::nullLiteral)
fun  OptionalBuilder<out NullPatternTree>.nullLiteral() = func(NullPatternTree::nullLiteral)
fun  ManyBuilder<out NullPatternTree>.nullLiteral() = func(NullPatternTree::nullLiteral)
