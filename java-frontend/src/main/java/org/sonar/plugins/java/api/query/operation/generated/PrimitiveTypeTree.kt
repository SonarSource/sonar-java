package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out PrimitiveTypeTree>.keyword() = func(PrimitiveTypeTree::keyword)
fun  OptionalBuilder<out PrimitiveTypeTree>.keyword() = func(PrimitiveTypeTree::keyword)
fun  ManyBuilder<out PrimitiveTypeTree>.keyword() = func(PrimitiveTypeTree::keyword)
