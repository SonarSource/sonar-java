package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.RecordPatternTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out RecordPatternTree>.closeParenToken() = func(RecordPatternTree::closeParenToken)
fun  OptionalBuilder<out RecordPatternTree>.closeParenToken() = func(RecordPatternTree::closeParenToken)
fun  ManyBuilder<out RecordPatternTree>.closeParenToken() = func(RecordPatternTree::closeParenToken)

fun  SingleBuilder<out RecordPatternTree>.listPatterns() = func(RecordPatternTree::patterns)
fun  OptionalBuilder<out RecordPatternTree>.listPatterns() = func(RecordPatternTree::patterns)
fun  ManyBuilder<out RecordPatternTree>.listPatterns() = func(RecordPatternTree::patterns)
fun  SingleBuilder<out RecordPatternTree>.patterns() = listFunc(RecordPatternTree::patterns)
fun  OptionalBuilder<out RecordPatternTree>.patterns() = listFunc(RecordPatternTree::patterns)
fun  ManyBuilder<out RecordPatternTree>.patterns() = listFunc(RecordPatternTree::patterns)

fun  SingleBuilder<out RecordPatternTree>.openParenToken() = func(RecordPatternTree::openParenToken)
fun  OptionalBuilder<out RecordPatternTree>.openParenToken() = func(RecordPatternTree::openParenToken)
fun  ManyBuilder<out RecordPatternTree>.openParenToken() = func(RecordPatternTree::openParenToken)

fun  SingleBuilder<out RecordPatternTree>.type() = func(RecordPatternTree::type)
fun  OptionalBuilder<out RecordPatternTree>.type() = func(RecordPatternTree::type)
fun  ManyBuilder<out RecordPatternTree>.type() = func(RecordPatternTree::type)
