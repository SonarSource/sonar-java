package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TypeParameters
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out TypeParameters>.closeBracketToken() = optFunc(TypeParameters::closeBracketToken)
fun  OptionalBuilder<out TypeParameters>.closeBracketToken() = optFunc(TypeParameters::closeBracketToken)
fun  ManyBuilder<out TypeParameters>.closeBracketToken() = optFunc(TypeParameters::closeBracketToken)

fun  SingleBuilder<out TypeParameters>.openBracketToken() = optFunc(TypeParameters::openBracketToken)
fun  OptionalBuilder<out TypeParameters>.openBracketToken() = optFunc(TypeParameters::openBracketToken)
fun  ManyBuilder<out TypeParameters>.openBracketToken() = optFunc(TypeParameters::openBracketToken)
