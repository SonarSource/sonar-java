package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.AnnotationTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out AnnotationTree>.annotationType() = func(AnnotationTree::annotationType)
fun  OptionalBuilder<out AnnotationTree>.annotationType() = func(AnnotationTree::annotationType)
fun  ManyBuilder<out AnnotationTree>.annotationType() = func(AnnotationTree::annotationType)

fun  SingleBuilder<out AnnotationTree>.atToken() = func(AnnotationTree::atToken)
fun  OptionalBuilder<out AnnotationTree>.atToken() = func(AnnotationTree::atToken)
fun  ManyBuilder<out AnnotationTree>.atToken() = func(AnnotationTree::atToken)

fun  SingleBuilder<out AnnotationTree>.listArguments() = func(AnnotationTree::arguments)
fun  OptionalBuilder<out AnnotationTree>.listArguments() = func(AnnotationTree::arguments)
fun  ManyBuilder<out AnnotationTree>.listArguments() = func(AnnotationTree::arguments)
fun  SingleBuilder<out AnnotationTree>.arguments() = listFunc(AnnotationTree::arguments)
fun  OptionalBuilder<out AnnotationTree>.arguments() = listFunc(AnnotationTree::arguments)
fun  ManyBuilder<out AnnotationTree>.arguments() = listFunc(AnnotationTree::arguments)
