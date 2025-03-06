package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TypeTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out TypeTree>.listAnnotations() = func(TypeTree::annotations)
fun  OptionalBuilder<out TypeTree>.listAnnotations() = func(TypeTree::annotations)
fun  ManyBuilder<out TypeTree>.listAnnotations() = func(TypeTree::annotations)
fun  SingleBuilder<out TypeTree>.annotations() = listFunc(TypeTree::annotations)
fun  OptionalBuilder<out TypeTree>.annotations() = listFunc(TypeTree::annotations)
fun  ManyBuilder<out TypeTree>.annotations() = listFunc(TypeTree::annotations)

fun  SingleBuilder<out TypeTree>.symbolType() = func(TypeTree::symbolType)
fun  OptionalBuilder<out TypeTree>.symbolType() = func(TypeTree::symbolType)
fun  ManyBuilder<out TypeTree>.symbolType() = func(TypeTree::symbolType)
