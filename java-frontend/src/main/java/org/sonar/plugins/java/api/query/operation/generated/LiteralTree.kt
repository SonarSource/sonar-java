package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.LiteralTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out LiteralTree>.token() = func(LiteralTree::token)
fun  OptionalBuilder<out LiteralTree>.token() = func(LiteralTree::token)
fun  ManyBuilder<out LiteralTree>.token() = func(LiteralTree::token)

fun  SingleBuilder<out LiteralTree>.value() = func(LiteralTree::value)
fun  OptionalBuilder<out LiteralTree>.value() = func(LiteralTree::value)
fun  ManyBuilder<out LiteralTree>.value() = func(LiteralTree::value)
