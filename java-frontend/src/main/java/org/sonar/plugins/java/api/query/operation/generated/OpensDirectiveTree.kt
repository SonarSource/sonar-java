package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.OpensDirectiveTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out OpensDirectiveTree>.listModuleNames() = func(OpensDirectiveTree::moduleNames)
fun  OptionalBuilder<out OpensDirectiveTree>.listModuleNames() = func(OpensDirectiveTree::moduleNames)
fun  ManyBuilder<out OpensDirectiveTree>.listModuleNames() = func(OpensDirectiveTree::moduleNames)
fun  SingleBuilder<out OpensDirectiveTree>.moduleNames() = listFunc(OpensDirectiveTree::moduleNames)
fun  OptionalBuilder<out OpensDirectiveTree>.moduleNames() = listFunc(OpensDirectiveTree::moduleNames)
fun  ManyBuilder<out OpensDirectiveTree>.moduleNames() = listFunc(OpensDirectiveTree::moduleNames)

fun  SingleBuilder<out OpensDirectiveTree>.packageName() = func(OpensDirectiveTree::packageName)
fun  OptionalBuilder<out OpensDirectiveTree>.packageName() = func(OpensDirectiveTree::packageName)
fun  ManyBuilder<out OpensDirectiveTree>.packageName() = func(OpensDirectiveTree::packageName)

fun  SingleBuilder<out OpensDirectiveTree>.toKeyword() = optFunc(OpensDirectiveTree::toKeyword)
fun  OptionalBuilder<out OpensDirectiveTree>.toKeyword() = optFunc(OpensDirectiveTree::toKeyword)
fun  ManyBuilder<out OpensDirectiveTree>.toKeyword() = optFunc(OpensDirectiveTree::toKeyword)
