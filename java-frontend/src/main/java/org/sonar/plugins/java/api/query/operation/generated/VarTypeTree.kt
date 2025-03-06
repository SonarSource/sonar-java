package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.VarTypeTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out VarTypeTree>.varToken() = func(VarTypeTree::varToken)
fun  OptionalBuilder<out VarTypeTree>.varToken() = func(VarTypeTree::varToken)
fun  ManyBuilder<out VarTypeTree>.varToken() = func(VarTypeTree::varToken)
