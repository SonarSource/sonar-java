package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ExportsDirectiveTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out ExportsDirectiveTree>.listModuleNames() = func(ExportsDirectiveTree::moduleNames)
fun  OptionalBuilder<out ExportsDirectiveTree>.listModuleNames() = func(ExportsDirectiveTree::moduleNames)
fun  ManyBuilder<out ExportsDirectiveTree>.listModuleNames() = func(ExportsDirectiveTree::moduleNames)
fun  SingleBuilder<out ExportsDirectiveTree>.moduleNames() = listFunc(ExportsDirectiveTree::moduleNames)
fun  OptionalBuilder<out ExportsDirectiveTree>.moduleNames() = listFunc(ExportsDirectiveTree::moduleNames)
fun  ManyBuilder<out ExportsDirectiveTree>.moduleNames() = listFunc(ExportsDirectiveTree::moduleNames)

fun  SingleBuilder<out ExportsDirectiveTree>.packageName() = func(ExportsDirectiveTree::packageName)
fun  OptionalBuilder<out ExportsDirectiveTree>.packageName() = func(ExportsDirectiveTree::packageName)
fun  ManyBuilder<out ExportsDirectiveTree>.packageName() = func(ExportsDirectiveTree::packageName)

fun  SingleBuilder<out ExportsDirectiveTree>.toKeyword() = optFunc(ExportsDirectiveTree::toKeyword)
fun  OptionalBuilder<out ExportsDirectiveTree>.toKeyword() = optFunc(ExportsDirectiveTree::toKeyword)
fun  ManyBuilder<out ExportsDirectiveTree>.toKeyword() = optFunc(ExportsDirectiveTree::toKeyword)
