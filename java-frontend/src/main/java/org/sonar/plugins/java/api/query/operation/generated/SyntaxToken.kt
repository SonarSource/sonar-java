package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.SyntaxToken
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out SyntaxToken>.listTrivias() = func(SyntaxToken::trivias)
fun  OptionalBuilder<out SyntaxToken>.listTrivias() = func(SyntaxToken::trivias)
fun  ManyBuilder<out SyntaxToken>.listTrivias() = func(SyntaxToken::trivias)
fun  SingleBuilder<out SyntaxToken>.trivias() = listFunc(SyntaxToken::trivias)
fun  OptionalBuilder<out SyntaxToken>.trivias() = listFunc(SyntaxToken::trivias)
fun  ManyBuilder<out SyntaxToken>.trivias() = listFunc(SyntaxToken::trivias)

fun  SingleBuilder<out SyntaxToken>.range() = func(SyntaxToken::range)
fun  OptionalBuilder<out SyntaxToken>.range() = func(SyntaxToken::range)
fun  ManyBuilder<out SyntaxToken>.range() = func(SyntaxToken::range)

fun  SingleBuilder<out SyntaxToken>.text() = func(SyntaxToken::text)
fun  OptionalBuilder<out SyntaxToken>.text() = func(SyntaxToken::text)
fun  ManyBuilder<out SyntaxToken>.text() = func(SyntaxToken::text)
