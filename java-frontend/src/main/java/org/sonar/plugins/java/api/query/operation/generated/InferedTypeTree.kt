package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.InferedTypeTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out InferedTypeTree>.firstToken() = optFunc(InferedTypeTree::firstToken)
fun  OptionalBuilder<out InferedTypeTree>.firstToken() = optFunc(InferedTypeTree::firstToken)
fun  ManyBuilder<out InferedTypeTree>.firstToken() = optFunc(InferedTypeTree::firstToken)

fun  SingleBuilder<out InferedTypeTree>.isLeaf() = func(InferedTypeTree::isLeaf)
fun  OptionalBuilder<out InferedTypeTree>.isLeaf() = func(InferedTypeTree::isLeaf)
fun  ManyBuilder<out InferedTypeTree>.isLeaf() = func(InferedTypeTree::isLeaf)

fun  SingleBuilder<out InferedTypeTree>.kind() = func(InferedTypeTree::kind)
fun  OptionalBuilder<out InferedTypeTree>.kind() = func(InferedTypeTree::kind)
fun  ManyBuilder<out InferedTypeTree>.kind() = func(InferedTypeTree::kind)

fun  SingleBuilder<out InferedTypeTree>.lastToken() = optFunc(InferedTypeTree::lastToken)
fun  OptionalBuilder<out InferedTypeTree>.lastToken() = optFunc(InferedTypeTree::lastToken)
fun  ManyBuilder<out InferedTypeTree>.lastToken() = optFunc(InferedTypeTree::lastToken)

fun  SingleBuilder<out InferedTypeTree>.listAnnotations() = func(InferedTypeTree::annotations)
fun  OptionalBuilder<out InferedTypeTree>.listAnnotations() = func(InferedTypeTree::annotations)
fun  ManyBuilder<out InferedTypeTree>.listAnnotations() = func(InferedTypeTree::annotations)
fun  SingleBuilder<out InferedTypeTree>.annotations() = listFunc(InferedTypeTree::annotations)
fun  OptionalBuilder<out InferedTypeTree>.annotations() = listFunc(InferedTypeTree::annotations)
fun  ManyBuilder<out InferedTypeTree>.annotations() = listFunc(InferedTypeTree::annotations)

fun  SingleBuilder<out InferedTypeTree>.listChildren() = func(InferedTypeTree::children)
fun  OptionalBuilder<out InferedTypeTree>.listChildren() = func(InferedTypeTree::children)
fun  ManyBuilder<out InferedTypeTree>.listChildren() = func(InferedTypeTree::children)
fun  SingleBuilder<out InferedTypeTree>.children() = listFunc(InferedTypeTree::children)
fun  OptionalBuilder<out InferedTypeTree>.children() = listFunc(InferedTypeTree::children)
fun  ManyBuilder<out InferedTypeTree>.children() = listFunc(InferedTypeTree::children)
