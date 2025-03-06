package org.sonar.plugins.java.api.query

import org.sonar.plugins.java.api.tree.BaseTreeVisitor
import org.sonar.plugins.java.api.tree.CompilationUnitTree
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.exec.ContextEntry

class TreeFlattening(
  val nodesInOrder: List<Tree>,
  val subTreeMap: Map<Tree, Pair<Int, Int>>
) {
  companion object {
    val contextEntry = ContextEntry<TreeFlattening>("tree_flattening")

    fun create(tree: CompilationUnitTree): TreeFlattening {
      val nodesInOrder = arrayListOf<Tree>()
      val subTreeMap = mutableMapOf<Tree, Pair<Int, Int>>()
      var counter = 0

      val visitor = object : BaseTreeVisitor() {
        public override fun scan(tree: Tree?) {
          if (tree != null) {
            nodesInOrder.add(tree)
            val start = counter++
            tree.accept(this)

            subTreeMap[tree] = (start + 1) to counter
          }
        }
      }

      visitor.scan(tree)

      return TreeFlattening(nodesInOrder, subTreeMap.toMap())
    }
  }

  fun getTree(
    tree: Tree,
    shouldTraverse: (Tree) -> Boolean = { true },
    includeStart: Boolean = true, // If true, yield the tree itself as the first parameter
    includeNonTraversed: Boolean = false // If true, yield the nodes that will not be traversed
  ): Sequence<Tree> {
    return sequence {
      if (includeStart) {
        yield(tree)
      }

      var (cur, end) = subTreeMap.getValue(tree)
      while (cur < end) {
        val node = nodesInOrder[cur]
        if (shouldTraverse(node)) {
          yield(node)
          cur++
        } else {
          if (includeNonTraversed) {
            yield(node)
          }

          cur = subTreeMap.getValue(node).second
        }
      }
    }
  }
}
