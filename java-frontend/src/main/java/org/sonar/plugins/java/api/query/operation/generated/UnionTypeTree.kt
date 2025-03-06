package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.UnionTypeTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out UnionTypeTree>.listTypeAlternatives() = func(UnionTypeTree::typeAlternatives)
fun  OptionalBuilder<out UnionTypeTree>.listTypeAlternatives() = func(UnionTypeTree::typeAlternatives)
fun  ManyBuilder<out UnionTypeTree>.listTypeAlternatives() = func(UnionTypeTree::typeAlternatives)
fun  SingleBuilder<out UnionTypeTree>.typeAlternatives() = listFunc(UnionTypeTree::typeAlternatives)
fun  OptionalBuilder<out UnionTypeTree>.typeAlternatives() = listFunc(UnionTypeTree::typeAlternatives)
fun  ManyBuilder<out UnionTypeTree>.typeAlternatives() = listFunc(UnionTypeTree::typeAlternatives)
