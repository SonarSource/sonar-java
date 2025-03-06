package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.EmptyStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out EmptyStatementTree>.semicolonToken() = func(EmptyStatementTree::semicolonToken)
fun  OptionalBuilder<out EmptyStatementTree>.semicolonToken() = func(EmptyStatementTree::semicolonToken)
fun  ManyBuilder<out EmptyStatementTree>.semicolonToken() = func(EmptyStatementTree::semicolonToken)
