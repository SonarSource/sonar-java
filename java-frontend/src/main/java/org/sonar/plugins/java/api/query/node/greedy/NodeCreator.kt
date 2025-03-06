package org.sonar.plugins.java.api.query.node.greedy

import org.sonar.plugins.java.api.query.operation.SubtreeFunction
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.exec.greedy.GreedyBuildCtx
import org.sonarsource.astquery.ir.nodes.FlatMap

object NodeCreator {

  fun createSubTreeNode(ctx: GreedyBuildCtx, ir: FlatMap<*, *>, func: SubtreeFunction): FastSubtreeNode {
    @Suppress("UNCHECKED_CAST")
    return FastSubtreeNode(ir.id, ctx.getChildren(ir as FlatMap<Tree, Tree>), func.stopAt, func.includeRoot)
  }

  fun createTreeParentNode(ctx: GreedyBuildCtx, ir: FlatMap<*, *>): TreeParentsNode {
    @Suppress("UNCHECKED_CAST")
    return TreeParentsNode(ir.id, ctx.getChildren(ir as FlatMap<Tree, Tree>))
  }
}
