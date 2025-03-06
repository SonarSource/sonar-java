package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ModifierKeywordTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ModifierKeywordTree>.keyword() = func(ModifierKeywordTree::keyword)
fun  OptionalBuilder<out ModifierKeywordTree>.keyword() = func(ModifierKeywordTree::keyword)
fun  ManyBuilder<out ModifierKeywordTree>.keyword() = func(ModifierKeywordTree::keyword)

fun  SingleBuilder<out ModifierKeywordTree>.modifier() = func(ModifierKeywordTree::modifier)
fun  OptionalBuilder<out ModifierKeywordTree>.modifier() = func(ModifierKeywordTree::modifier)
fun  ManyBuilder<out ModifierKeywordTree>.modifier() = func(ModifierKeywordTree::modifier)
