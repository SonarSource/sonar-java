package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out ParameterizedTypeTree>.listTypeArguments() = func(ParameterizedTypeTree::typeArguments)
fun  OptionalBuilder<out ParameterizedTypeTree>.listTypeArguments() = func(ParameterizedTypeTree::typeArguments)
fun  ManyBuilder<out ParameterizedTypeTree>.listTypeArguments() = func(ParameterizedTypeTree::typeArguments)
fun  SingleBuilder<out ParameterizedTypeTree>.typeArguments() = listFunc(ParameterizedTypeTree::typeArguments)
fun  OptionalBuilder<out ParameterizedTypeTree>.typeArguments() = listFunc(ParameterizedTypeTree::typeArguments)
fun  ManyBuilder<out ParameterizedTypeTree>.typeArguments() = listFunc(ParameterizedTypeTree::typeArguments)

fun  SingleBuilder<out ParameterizedTypeTree>.type() = func(ParameterizedTypeTree::type)
fun  OptionalBuilder<out ParameterizedTypeTree>.type() = func(ParameterizedTypeTree::type)
fun  ManyBuilder<out ParameterizedTypeTree>.type() = func(ParameterizedTypeTree::type)
