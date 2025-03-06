package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ModuleDirectiveTree>.directiveKeyword() = func(ModuleDirectiveTree::directiveKeyword)
fun  OptionalBuilder<out ModuleDirectiveTree>.directiveKeyword() = func(ModuleDirectiveTree::directiveKeyword)
fun  ManyBuilder<out ModuleDirectiveTree>.directiveKeyword() = func(ModuleDirectiveTree::directiveKeyword)

fun  SingleBuilder<out ModuleDirectiveTree>.semicolonToken() = func(ModuleDirectiveTree::semicolonToken)
fun  OptionalBuilder<out ModuleDirectiveTree>.semicolonToken() = func(ModuleDirectiveTree::semicolonToken)
fun  ManyBuilder<out ModuleDirectiveTree>.semicolonToken() = func(ModuleDirectiveTree::semicolonToken)
