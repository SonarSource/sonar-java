package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TypeArguments
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out TypeArguments>.closeBracketToken() = func(TypeArguments::closeBracketToken)
fun  OptionalBuilder<out TypeArguments>.closeBracketToken() = func(TypeArguments::closeBracketToken)
fun  ManyBuilder<out TypeArguments>.closeBracketToken() = func(TypeArguments::closeBracketToken)

fun  SingleBuilder<out TypeArguments>.openBracketToken() = func(TypeArguments::openBracketToken)
fun  OptionalBuilder<out TypeArguments>.openBracketToken() = func(TypeArguments::openBracketToken)
fun  ManyBuilder<out TypeArguments>.openBracketToken() = func(TypeArguments::openBracketToken)
