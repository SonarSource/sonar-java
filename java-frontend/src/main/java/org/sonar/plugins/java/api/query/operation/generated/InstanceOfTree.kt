package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.InstanceOfTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out InstanceOfTree>.expression() = func(InstanceOfTree::expression)
fun  OptionalBuilder<out InstanceOfTree>.expression() = func(InstanceOfTree::expression)
fun  ManyBuilder<out InstanceOfTree>.expression() = func(InstanceOfTree::expression)

fun  SingleBuilder<out InstanceOfTree>.instanceofKeyword() = func(InstanceOfTree::instanceofKeyword)
fun  OptionalBuilder<out InstanceOfTree>.instanceofKeyword() = func(InstanceOfTree::instanceofKeyword)
fun  ManyBuilder<out InstanceOfTree>.instanceofKeyword() = func(InstanceOfTree::instanceofKeyword)

fun  SingleBuilder<out InstanceOfTree>.type() = func(InstanceOfTree::type)
fun  OptionalBuilder<out InstanceOfTree>.type() = func(InstanceOfTree::type)
fun  ManyBuilder<out InstanceOfTree>.type() = func(InstanceOfTree::type)
