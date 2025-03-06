package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ModifiersTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out ModifiersTree>.listAnnotations() = func(ModifiersTree::annotations)
fun  OptionalBuilder<out ModifiersTree>.listAnnotations() = func(ModifiersTree::annotations)
fun  ManyBuilder<out ModifiersTree>.listAnnotations() = func(ModifiersTree::annotations)
fun  SingleBuilder<out ModifiersTree>.annotations() = listFunc(ModifiersTree::annotations)
fun  OptionalBuilder<out ModifiersTree>.annotations() = listFunc(ModifiersTree::annotations)
fun  ManyBuilder<out ModifiersTree>.annotations() = listFunc(ModifiersTree::annotations)

fun  SingleBuilder<out ModifiersTree>.listModifiers() = func(ModifiersTree::modifiers)
fun  OptionalBuilder<out ModifiersTree>.listModifiers() = func(ModifiersTree::modifiers)
fun  ManyBuilder<out ModifiersTree>.listModifiers() = func(ModifiersTree::modifiers)
fun  SingleBuilder<out ModifiersTree>.modifiers() = listFunc(ModifiersTree::modifiers)
fun  OptionalBuilder<out ModifiersTree>.modifiers() = listFunc(ModifiersTree::modifiers)
fun  ManyBuilder<out ModifiersTree>.modifiers() = listFunc(ModifiersTree::modifiers)
