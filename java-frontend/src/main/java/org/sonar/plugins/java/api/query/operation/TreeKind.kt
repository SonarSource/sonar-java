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
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonarsource.astquery.ir.IdentifiedNodeFunction
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.core.filter

data class TreeIsOfKindFunction(val kinds: Set<Tree.Kind>) : IdentifiedNodeFunction<(Tree) -> Boolean>("Tree.IsOfKind")

@Suppress("UNCHECKED_CAST")
fun <I : Tree, O : Tree> SingleBuilder<I>.ofKind(vararg kinds: TreeKind<out O>): OptionalBuilder<O> =
  filter { tree -> kinds.any { it.kind == tree.kind() } } as OptionalBuilder<O>

@Suppress("UNCHECKED_CAST")
fun <I : Tree, O : Tree> OptionalBuilder<I>.ofKind(vararg kinds: TreeKind<out O>): OptionalBuilder<O> =
  filter { tree -> kinds.any { it.kind == tree.kind() } } as OptionalBuilder<O>

@Suppress("UNCHECKED_CAST")
fun <I : Tree, O : Tree> ManyBuilder<I>.ofKind(vararg kinds: TreeKind<out O>): ManyBuilder<O> =
  filter { tree -> kinds.any { it.kind == tree.kind() } } as ManyBuilder<O>
