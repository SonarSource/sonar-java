package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ListTree
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun <A> SingleBuilder<out ListTree<A>>.listSeparators() where A: Tree = func(ListTree<A>::separators)
fun <A> OptionalBuilder<out ListTree<A>>.listSeparators() where A: Tree = func(ListTree<A>::separators)
fun <A> ManyBuilder<out ListTree<A>>.listSeparators() where A: Tree = func(ListTree<A>::separators)
fun <A> SingleBuilder<out ListTree<A>>.separators() where A: Tree = listFunc(ListTree<A>::separators)
fun <A> OptionalBuilder<out ListTree<A>>.separators() where A: Tree = listFunc(ListTree<A>::separators)
fun <A> ManyBuilder<out ListTree<A>>.separators() where A: Tree = listFunc(ListTree<A>::separators)
