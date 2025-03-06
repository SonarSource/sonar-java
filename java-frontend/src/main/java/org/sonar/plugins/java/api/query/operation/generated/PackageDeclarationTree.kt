package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.PackageDeclarationTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out PackageDeclarationTree>.listAnnotations() = func(PackageDeclarationTree::annotations)
fun  OptionalBuilder<out PackageDeclarationTree>.listAnnotations() = func(PackageDeclarationTree::annotations)
fun  ManyBuilder<out PackageDeclarationTree>.listAnnotations() = func(PackageDeclarationTree::annotations)
fun  SingleBuilder<out PackageDeclarationTree>.annotations() = listFunc(PackageDeclarationTree::annotations)
fun  OptionalBuilder<out PackageDeclarationTree>.annotations() = listFunc(PackageDeclarationTree::annotations)
fun  ManyBuilder<out PackageDeclarationTree>.annotations() = listFunc(PackageDeclarationTree::annotations)

fun  SingleBuilder<out PackageDeclarationTree>.packageKeyword() = func(PackageDeclarationTree::packageKeyword)
fun  OptionalBuilder<out PackageDeclarationTree>.packageKeyword() = func(PackageDeclarationTree::packageKeyword)
fun  ManyBuilder<out PackageDeclarationTree>.packageKeyword() = func(PackageDeclarationTree::packageKeyword)

fun  SingleBuilder<out PackageDeclarationTree>.packageName() = func(PackageDeclarationTree::packageName)
fun  OptionalBuilder<out PackageDeclarationTree>.packageName() = func(PackageDeclarationTree::packageName)
fun  ManyBuilder<out PackageDeclarationTree>.packageName() = func(PackageDeclarationTree::packageName)

fun  SingleBuilder<out PackageDeclarationTree>.semicolonToken() = func(PackageDeclarationTree::semicolonToken)
fun  OptionalBuilder<out PackageDeclarationTree>.semicolonToken() = func(PackageDeclarationTree::semicolonToken)
fun  ManyBuilder<out PackageDeclarationTree>.semicolonToken() = func(PackageDeclarationTree::semicolonToken)
