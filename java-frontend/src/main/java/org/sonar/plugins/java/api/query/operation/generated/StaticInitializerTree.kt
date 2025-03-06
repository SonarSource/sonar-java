package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.StaticInitializerTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out StaticInitializerTree>.staticKeyword() = func(StaticInitializerTree::staticKeyword)
fun  OptionalBuilder<out StaticInitializerTree>.staticKeyword() = func(StaticInitializerTree::staticKeyword)
fun  ManyBuilder<out StaticInitializerTree>.staticKeyword() = func(StaticInitializerTree::staticKeyword)
