package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out ModuleDeclarationTree>.closeBraceToken() = func(ModuleDeclarationTree::closeBraceToken)
fun  OptionalBuilder<out ModuleDeclarationTree>.closeBraceToken() = func(ModuleDeclarationTree::closeBraceToken)
fun  ManyBuilder<out ModuleDeclarationTree>.closeBraceToken() = func(ModuleDeclarationTree::closeBraceToken)

fun  SingleBuilder<out ModuleDeclarationTree>.listAnnotations() = func(ModuleDeclarationTree::annotations)
fun  OptionalBuilder<out ModuleDeclarationTree>.listAnnotations() = func(ModuleDeclarationTree::annotations)
fun  ManyBuilder<out ModuleDeclarationTree>.listAnnotations() = func(ModuleDeclarationTree::annotations)
fun  SingleBuilder<out ModuleDeclarationTree>.annotations() = listFunc(ModuleDeclarationTree::annotations)
fun  OptionalBuilder<out ModuleDeclarationTree>.annotations() = listFunc(ModuleDeclarationTree::annotations)
fun  ManyBuilder<out ModuleDeclarationTree>.annotations() = listFunc(ModuleDeclarationTree::annotations)

fun  SingleBuilder<out ModuleDeclarationTree>.listModuleDirectives() = func(ModuleDeclarationTree::moduleDirectives)
fun  OptionalBuilder<out ModuleDeclarationTree>.listModuleDirectives() = func(ModuleDeclarationTree::moduleDirectives)
fun  ManyBuilder<out ModuleDeclarationTree>.listModuleDirectives() = func(ModuleDeclarationTree::moduleDirectives)
fun  SingleBuilder<out ModuleDeclarationTree>.moduleDirectives() = listFunc(ModuleDeclarationTree::moduleDirectives)
fun  OptionalBuilder<out ModuleDeclarationTree>.moduleDirectives() = listFunc(ModuleDeclarationTree::moduleDirectives)
fun  ManyBuilder<out ModuleDeclarationTree>.moduleDirectives() = listFunc(ModuleDeclarationTree::moduleDirectives)

fun  SingleBuilder<out ModuleDeclarationTree>.listModuleName() = func(ModuleDeclarationTree::moduleName)
fun  OptionalBuilder<out ModuleDeclarationTree>.listModuleName() = func(ModuleDeclarationTree::moduleName)
fun  ManyBuilder<out ModuleDeclarationTree>.listModuleName() = func(ModuleDeclarationTree::moduleName)
fun  SingleBuilder<out ModuleDeclarationTree>.moduleName() = listFunc(ModuleDeclarationTree::moduleName)
fun  OptionalBuilder<out ModuleDeclarationTree>.moduleName() = listFunc(ModuleDeclarationTree::moduleName)
fun  ManyBuilder<out ModuleDeclarationTree>.moduleName() = listFunc(ModuleDeclarationTree::moduleName)

fun  SingleBuilder<out ModuleDeclarationTree>.moduleKeyword() = func(ModuleDeclarationTree::moduleKeyword)
fun  OptionalBuilder<out ModuleDeclarationTree>.moduleKeyword() = func(ModuleDeclarationTree::moduleKeyword)
fun  ManyBuilder<out ModuleDeclarationTree>.moduleKeyword() = func(ModuleDeclarationTree::moduleKeyword)

fun  SingleBuilder<out ModuleDeclarationTree>.openBraceToken() = func(ModuleDeclarationTree::openBraceToken)
fun  OptionalBuilder<out ModuleDeclarationTree>.openBraceToken() = func(ModuleDeclarationTree::openBraceToken)
fun  ManyBuilder<out ModuleDeclarationTree>.openBraceToken() = func(ModuleDeclarationTree::openBraceToken)

fun  SingleBuilder<out ModuleDeclarationTree>.openKeyword() = optFunc(ModuleDeclarationTree::openKeyword)
fun  OptionalBuilder<out ModuleDeclarationTree>.openKeyword() = optFunc(ModuleDeclarationTree::openKeyword)
fun  ManyBuilder<out ModuleDeclarationTree>.openKeyword() = optFunc(ModuleDeclarationTree::openKeyword)
