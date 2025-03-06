package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.UsesDirectiveTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out UsesDirectiveTree>.typeName() = func(UsesDirectiveTree::typeName)
fun  OptionalBuilder<out UsesDirectiveTree>.typeName() = func(UsesDirectiveTree::typeName)
fun  ManyBuilder<out UsesDirectiveTree>.typeName() = func(UsesDirectiveTree::typeName)
