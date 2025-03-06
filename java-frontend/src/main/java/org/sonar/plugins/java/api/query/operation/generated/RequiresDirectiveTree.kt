package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out RequiresDirectiveTree>.listModifiers() = func(RequiresDirectiveTree::modifiers)
fun  OptionalBuilder<out RequiresDirectiveTree>.listModifiers() = func(RequiresDirectiveTree::modifiers)
fun  ManyBuilder<out RequiresDirectiveTree>.listModifiers() = func(RequiresDirectiveTree::modifiers)
fun  SingleBuilder<out RequiresDirectiveTree>.modifiers() = listFunc(RequiresDirectiveTree::modifiers)
fun  OptionalBuilder<out RequiresDirectiveTree>.modifiers() = listFunc(RequiresDirectiveTree::modifiers)
fun  ManyBuilder<out RequiresDirectiveTree>.modifiers() = listFunc(RequiresDirectiveTree::modifiers)

fun  SingleBuilder<out RequiresDirectiveTree>.listModuleName() = func(RequiresDirectiveTree::moduleName)
fun  OptionalBuilder<out RequiresDirectiveTree>.listModuleName() = func(RequiresDirectiveTree::moduleName)
fun  ManyBuilder<out RequiresDirectiveTree>.listModuleName() = func(RequiresDirectiveTree::moduleName)
fun  SingleBuilder<out RequiresDirectiveTree>.moduleName() = listFunc(RequiresDirectiveTree::moduleName)
fun  OptionalBuilder<out RequiresDirectiveTree>.moduleName() = listFunc(RequiresDirectiveTree::moduleName)
fun  ManyBuilder<out RequiresDirectiveTree>.moduleName() = listFunc(RequiresDirectiveTree::moduleName)
