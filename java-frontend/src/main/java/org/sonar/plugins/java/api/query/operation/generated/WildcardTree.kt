package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.WildcardTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out WildcardTree>.bound() = optFunc(WildcardTree::bound)
fun  OptionalBuilder<out WildcardTree>.bound() = optFunc(WildcardTree::bound)
fun  ManyBuilder<out WildcardTree>.bound() = optFunc(WildcardTree::bound)

fun  SingleBuilder<out WildcardTree>.extendsOrSuperToken() = optFunc(WildcardTree::extendsOrSuperToken)
fun  OptionalBuilder<out WildcardTree>.extendsOrSuperToken() = optFunc(WildcardTree::extendsOrSuperToken)
fun  ManyBuilder<out WildcardTree>.extendsOrSuperToken() = optFunc(WildcardTree::extendsOrSuperToken)

fun  SingleBuilder<out WildcardTree>.listAnnotations() = func(WildcardTree::annotations)
fun  OptionalBuilder<out WildcardTree>.listAnnotations() = func(WildcardTree::annotations)
fun  ManyBuilder<out WildcardTree>.listAnnotations() = func(WildcardTree::annotations)
fun  SingleBuilder<out WildcardTree>.annotations() = listFunc(WildcardTree::annotations)
fun  OptionalBuilder<out WildcardTree>.annotations() = listFunc(WildcardTree::annotations)
fun  ManyBuilder<out WildcardTree>.annotations() = listFunc(WildcardTree::annotations)

fun  SingleBuilder<out WildcardTree>.queryToken() = func(WildcardTree::queryToken)
fun  OptionalBuilder<out WildcardTree>.queryToken() = func(WildcardTree::queryToken)
fun  ManyBuilder<out WildcardTree>.queryToken() = func(WildcardTree::queryToken)
