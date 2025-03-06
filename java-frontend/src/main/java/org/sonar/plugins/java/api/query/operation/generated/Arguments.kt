package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.Arguments
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out Arguments>.closeParenToken() = optFunc(Arguments::closeParenToken)
fun  OptionalBuilder<out Arguments>.closeParenToken() = optFunc(Arguments::closeParenToken)
fun  ManyBuilder<out Arguments>.closeParenToken() = optFunc(Arguments::closeParenToken)

fun  SingleBuilder<out Arguments>.openParenToken() = optFunc(Arguments::openParenToken)
fun  OptionalBuilder<out Arguments>.openParenToken() = optFunc(Arguments::openParenToken)
fun  ManyBuilder<out Arguments>.openParenToken() = optFunc(Arguments::openParenToken)
