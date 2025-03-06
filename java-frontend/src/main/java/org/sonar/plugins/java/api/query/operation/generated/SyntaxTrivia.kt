package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.SyntaxTrivia
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out SyntaxTrivia>.comment() = func(SyntaxTrivia::comment)
fun  OptionalBuilder<out SyntaxTrivia>.comment() = func(SyntaxTrivia::comment)
fun  ManyBuilder<out SyntaxTrivia>.comment() = func(SyntaxTrivia::comment)

fun  SingleBuilder<out SyntaxTrivia>.range() = func(SyntaxTrivia::range)
fun  OptionalBuilder<out SyntaxTrivia>.range() = func(SyntaxTrivia::range)
fun  ManyBuilder<out SyntaxTrivia>.range() = func(SyntaxTrivia::range)
