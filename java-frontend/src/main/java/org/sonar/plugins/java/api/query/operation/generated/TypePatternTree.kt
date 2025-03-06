package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TypePatternTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out TypePatternTree>.patternVariable() = func(TypePatternTree::patternVariable)
fun  OptionalBuilder<out TypePatternTree>.patternVariable() = func(TypePatternTree::patternVariable)
fun  ManyBuilder<out TypePatternTree>.patternVariable() = func(TypePatternTree::patternVariable)
