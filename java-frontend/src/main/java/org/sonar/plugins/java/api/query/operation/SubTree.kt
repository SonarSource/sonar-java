/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.plugins.java.api.query.operation

import org.sonar.plugins.java.api.tree.Tree
import org.sonar.plugins.java.api.tree.Tree.Kind
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonarsource.astquery.ir.IdentifiedFunction
import org.sonarsource.astquery.ir.IdentifiedNodeFunction
import org.sonarsource.astquery.ir.constant
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.core.flatMapSeq

data class SubtreeFunction(
  val includeRoot: Boolean = false,
  val stopAt: Set<Kind>
) : IdentifiedNodeFunction<(Tree) -> Sequence<Tree>>(
  if (includeRoot) "Tree" else "SubTree" + stopAt.joinToString(", ", "(", ")")
)

class TreeParameters {
  var stopAt: IdentifiedFunction<(Tree) -> Boolean> = constant(false, "Nothing")

  fun stopAt(vararg kinds: Kind) = stopAt(kinds.toSet())

  fun stopAt(kinds: Set<Kind>) {
    this.stopAt = TreeIsOfKindFunction(kinds)
  }
}

fun <T : Tree> SingleBuilder<T>.subtree() = flatMapSeq(SubtreeFunction(false, emptySet()))
fun <T : Tree> OptionalBuilder<T>.subtree() = flatMapSeq(SubtreeFunction(false, emptySet()))
fun <T : Tree> ManyBuilder<T>.subtree() = flatMapSeq(SubtreeFunction(false, emptySet()))

fun <T : Tree> SingleBuilder<T>.tree() = flatMapSeq(SubtreeFunction(true, emptySet()))
fun <T : Tree> OptionalBuilder<T>.tree() = flatMapSeq(SubtreeFunction(true, emptySet()))
fun <T : Tree> ManyBuilder<T>.tree() = flatMapSeq(SubtreeFunction(true, emptySet()))

fun <T : Tree> SingleBuilder<T>.subtreeStoppingAt(vararg kinds: Kind) =
  flatMapSeq(SubtreeFunction(false, kinds.toSet()))

fun <T : Tree> OptionalBuilder<T>.subtreeStoppingAt(vararg kinds: Kind) =
  flatMapSeq(SubtreeFunction(false, kinds.toSet()))

fun <T : Tree> ManyBuilder<T>.subtreeStoppingAt(vararg kinds: Kind) =
  flatMapSeq(SubtreeFunction(false, kinds.toSet()))

fun <T : Tree> SingleBuilder<T>.treeStoppingAt(vararg kinds: Kind) =
  flatMapSeq(SubtreeFunction(true, kinds.toSet()))

fun <T : Tree> OptionalBuilder<T>.treeStoppingAt(vararg kinds: Kind) =
  flatMapSeq(SubtreeFunction(true, kinds.toSet()))

fun <T : Tree> ManyBuilder<T>.treeStoppingAt(vararg kinds: Kind) =
  flatMapSeq(SubtreeFunction(true, kinds.toSet()))

fun <T : Tree> SingleBuilder<T>.subtreeStoppingAt(kinds: Set<Kind>) =
  flatMapSeq(SubtreeFunction(false, kinds))

fun <T : Tree> OptionalBuilder<T>.subtreeStoppingAt(kinds: Set<Kind>) =
  flatMapSeq(SubtreeFunction(false, kinds))

fun <T : Tree> ManyBuilder<T>.subtreeStoppingAt(kinds: Set<Kind>) =
  flatMapSeq(SubtreeFunction(false, kinds))

@JvmName("subTree-TK")
fun <T : Tree> SingleBuilder<T>.subtreeStoppingAt(vararg kinds: TreeKind<*>) =
  flatMapSeq(SubtreeFunction(false, kinds.map { it.kind }.toSet()))

@JvmName("subTree-TK")
fun <T : Tree> OptionalBuilder<T>.subtreeStoppingAt(vararg kinds: TreeKind<*>) =
  flatMapSeq(SubtreeFunction(false, kinds.map { it.kind }.toSet()))

@JvmName("subTree-TK")
fun <T : Tree> ManyBuilder<T>.subtreeStoppingAt(vararg kinds: TreeKind<*>) =
  flatMapSeq(SubtreeFunction(false, kinds.map { it.kind }.toSet()))

@JvmName("subTree-TK")
fun <T : Tree> SingleBuilder<T>.subtreeStoppingAt(kinds: Set<TreeKind<*>>) =
  flatMapSeq(SubtreeFunction(false, kinds.map { it.kind }.toSet()))

@JvmName("subTree-TK")
fun <T : Tree> OptionalBuilder<T>.subtreeStoppingAt(kinds: Set<TreeKind<*>>) =
  flatMapSeq(SubtreeFunction(false, kinds.map { it.kind }.toSet()))

@JvmName("subTree-TK")
fun <T : Tree> ManyBuilder<T>.subtreeStoppingAt(kinds: Set<TreeKind<*>>) =
  flatMapSeq(SubtreeFunction(false, kinds.map { it.kind }.toSet()))
