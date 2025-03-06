package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ClassTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out ClassTree>.closeBraceToken() = func(ClassTree::closeBraceToken)
fun  OptionalBuilder<out ClassTree>.closeBraceToken() = func(ClassTree::closeBraceToken)
fun  ManyBuilder<out ClassTree>.closeBraceToken() = func(ClassTree::closeBraceToken)

fun  SingleBuilder<out ClassTree>.declarationKeyword() = optFunc(ClassTree::declarationKeyword)
fun  OptionalBuilder<out ClassTree>.declarationKeyword() = optFunc(ClassTree::declarationKeyword)
fun  ManyBuilder<out ClassTree>.declarationKeyword() = optFunc(ClassTree::declarationKeyword)

fun  SingleBuilder<out ClassTree>.listMembers() = func(ClassTree::members)
fun  OptionalBuilder<out ClassTree>.listMembers() = func(ClassTree::members)
fun  ManyBuilder<out ClassTree>.listMembers() = func(ClassTree::members)
fun  SingleBuilder<out ClassTree>.members() = listFunc(ClassTree::members)
fun  OptionalBuilder<out ClassTree>.members() = listFunc(ClassTree::members)
fun  ManyBuilder<out ClassTree>.members() = listFunc(ClassTree::members)

fun  SingleBuilder<out ClassTree>.listModifiers() = func(ClassTree::modifiers)
fun  OptionalBuilder<out ClassTree>.listModifiers() = func(ClassTree::modifiers)
fun  ManyBuilder<out ClassTree>.listModifiers() = func(ClassTree::modifiers)
fun  SingleBuilder<out ClassTree>.modifiers() = listFunc(ClassTree::modifiers)
fun  OptionalBuilder<out ClassTree>.modifiers() = listFunc(ClassTree::modifiers)
fun  ManyBuilder<out ClassTree>.modifiers() = listFunc(ClassTree::modifiers)

fun  SingleBuilder<out ClassTree>.listPermittedTypes() = func(ClassTree::permittedTypes)
fun  OptionalBuilder<out ClassTree>.listPermittedTypes() = func(ClassTree::permittedTypes)
fun  ManyBuilder<out ClassTree>.listPermittedTypes() = func(ClassTree::permittedTypes)
fun  SingleBuilder<out ClassTree>.permittedTypes() = listFunc(ClassTree::permittedTypes)
fun  OptionalBuilder<out ClassTree>.permittedTypes() = listFunc(ClassTree::permittedTypes)
fun  ManyBuilder<out ClassTree>.permittedTypes() = listFunc(ClassTree::permittedTypes)

fun  SingleBuilder<out ClassTree>.listRecordComponents() = func(ClassTree::recordComponents)
fun  OptionalBuilder<out ClassTree>.listRecordComponents() = func(ClassTree::recordComponents)
fun  ManyBuilder<out ClassTree>.listRecordComponents() = func(ClassTree::recordComponents)
fun  SingleBuilder<out ClassTree>.recordComponents() = listFunc(ClassTree::recordComponents)
fun  OptionalBuilder<out ClassTree>.recordComponents() = listFunc(ClassTree::recordComponents)
fun  ManyBuilder<out ClassTree>.recordComponents() = listFunc(ClassTree::recordComponents)

fun  SingleBuilder<out ClassTree>.listSuperInterfaces() = func(ClassTree::superInterfaces)
fun  OptionalBuilder<out ClassTree>.listSuperInterfaces() = func(ClassTree::superInterfaces)
fun  ManyBuilder<out ClassTree>.listSuperInterfaces() = func(ClassTree::superInterfaces)
fun  SingleBuilder<out ClassTree>.superInterfaces() = listFunc(ClassTree::superInterfaces)
fun  OptionalBuilder<out ClassTree>.superInterfaces() = listFunc(ClassTree::superInterfaces)
fun  ManyBuilder<out ClassTree>.superInterfaces() = listFunc(ClassTree::superInterfaces)

fun  SingleBuilder<out ClassTree>.listTypeParameters() = func(ClassTree::typeParameters)
fun  OptionalBuilder<out ClassTree>.listTypeParameters() = func(ClassTree::typeParameters)
fun  ManyBuilder<out ClassTree>.listTypeParameters() = func(ClassTree::typeParameters)
fun  SingleBuilder<out ClassTree>.typeParameters() = listFunc(ClassTree::typeParameters)
fun  OptionalBuilder<out ClassTree>.typeParameters() = listFunc(ClassTree::typeParameters)
fun  ManyBuilder<out ClassTree>.typeParameters() = listFunc(ClassTree::typeParameters)

fun  SingleBuilder<out ClassTree>.openBraceToken() = func(ClassTree::openBraceToken)
fun  OptionalBuilder<out ClassTree>.openBraceToken() = func(ClassTree::openBraceToken)
fun  ManyBuilder<out ClassTree>.openBraceToken() = func(ClassTree::openBraceToken)

fun  SingleBuilder<out ClassTree>.permitsKeyword() = optFunc(ClassTree::permitsKeyword)
fun  OptionalBuilder<out ClassTree>.permitsKeyword() = optFunc(ClassTree::permitsKeyword)
fun  ManyBuilder<out ClassTree>.permitsKeyword() = optFunc(ClassTree::permitsKeyword)

fun  SingleBuilder<out ClassTree>.recordCloseParenToken() = optFunc(ClassTree::recordCloseParenToken)
fun  OptionalBuilder<out ClassTree>.recordCloseParenToken() = optFunc(ClassTree::recordCloseParenToken)
fun  ManyBuilder<out ClassTree>.recordCloseParenToken() = optFunc(ClassTree::recordCloseParenToken)

fun  SingleBuilder<out ClassTree>.recordOpenParenToken() = optFunc(ClassTree::recordOpenParenToken)
fun  OptionalBuilder<out ClassTree>.recordOpenParenToken() = optFunc(ClassTree::recordOpenParenToken)
fun  ManyBuilder<out ClassTree>.recordOpenParenToken() = optFunc(ClassTree::recordOpenParenToken)

fun  SingleBuilder<out ClassTree>.simpleName() = optFunc(ClassTree::simpleName)
fun  OptionalBuilder<out ClassTree>.simpleName() = optFunc(ClassTree::simpleName)
fun  ManyBuilder<out ClassTree>.simpleName() = optFunc(ClassTree::simpleName)

fun  SingleBuilder<out ClassTree>.superClass() = optFunc(ClassTree::superClass)
fun  OptionalBuilder<out ClassTree>.superClass() = optFunc(ClassTree::superClass)
fun  ManyBuilder<out ClassTree>.superClass() = optFunc(ClassTree::superClass)

fun  SingleBuilder<out ClassTree>.symbol() = func(ClassTree::symbol)
fun  OptionalBuilder<out ClassTree>.symbol() = func(ClassTree::symbol)
fun  ManyBuilder<out ClassTree>.symbol() = func(ClassTree::symbol)
