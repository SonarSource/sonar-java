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

package org.sonar.plugins.java.api.query.node.batch

import org.sonar.plugins.java.api.tree.BaseTreeVisitor
import org.sonar.plugins.java.api.tree.Tree
import org.sonar.plugins.java.api.tree.Tree.Kind
import org.sonarsource.astquery.exec.ExecutionContext
import org.sonarsource.astquery.exec.batch.BatchNode
import org.sonarsource.astquery.exec.batch.ChildNode
import org.sonarsource.astquery.exec.batch.Signal
import org.sonarsource.astquery.graph.NodeId
import org.sonarsource.astquery.graph.visual.FlowType
import org.sonarsource.astquery.graph.visual.VisualInfo

class SubtreeNode(
  id: NodeId,
  children: List<ChildNode<Tree>>,
  val stopAt: Set<Kind>,
  val includeStart: Boolean,
) : BatchNode<Tree, Tree>(id, children) {

  override val isSink = false

  override fun onValue(context: ExecutionContext, caller: NodeId, value: Signal.Value<Tree>) {
    val visitor = Visitor(stopAt)
    value.values.forEach { t ->
      if (includeStart) {
        visitor.collector.add(t)
      }

      t.accept(visitor)
    }

    propagate(context, Signal.Value(visitor.collector, value.scopes))
  }

  private class Visitor(
    private val stopAt: Set<Kind>
  ) : BaseTreeVisitor() {

    val collector = mutableListOf<Tree>()

    override fun scan(tree: Tree?) {
      tree ?: return

      collector.add(tree)

      if (!tree.`is`(*stopAt.toTypedArray())) {
        super.scan(tree)
      }
    }
  }

  override fun getFlowType(parentsInfo: Map<BatchNode<*, *>, VisualInfo>) = FlowType.Many

  override fun toString() = "${if (includeStart) "Tree" else "Subtree"}${stopAt.joinToString(", ", "(", ")")}-$id"
}
