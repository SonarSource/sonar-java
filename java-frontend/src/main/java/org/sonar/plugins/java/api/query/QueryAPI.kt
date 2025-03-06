package org.sonar.plugins.java.api.query

import org.sonar.plugins.java.api.JavaFileScannerContext
import org.sonar.plugins.java.api.query.node.greedy.NodeCreator
import org.sonar.plugins.java.api.query.operation.SubtreeFunction
import org.sonar.plugins.java.api.query.operation.TreeParentFunction
import org.sonar.plugins.java.api.query.operation.parserContextEntry
import org.sonar.plugins.java.api.query.operation.tree
import org.sonarsource.astquery.PipelineManager
import org.sonarsource.astquery.exec.greedy.GreedyBuilderFactory
import org.sonarsource.astquery.ir.nodes.FlatMap
import org.sonarsource.astquery.operation.composite.func

object QueryAPI {

  fun getPipelineManager(): PipelineManager<JavaFileScannerContext> =
    PipelineManager.Builder<JavaFileScannerContext>()
      .withExecBuilder(
        GreedyBuilderFactory()
          .registerSpecializedNode<FlatMap<*, *>, SubtreeFunction> { ctx, ir, func ->
            NodeCreator.createSubTreeNode(ctx, ir, func)
          }
          .registerSpecializedNode<FlatMap<*, *>, TreeParentFunction> { ctx, ir, _ ->
            NodeCreator.createTreeParentNode(ctx, ir)
          }
      )
      .addMetadata(TreeFlattening.contextEntry) { parser -> TreeFlattening.create(parser.getTree()) }
      .addMetadata(parserContextEntry) { parser -> parser }
      .build()

  fun prepareQuery(pipelineManager: PipelineManager<JavaFileScannerContext>, queryRule: QueryRule) {
    pipelineManager.registerPipeline { ctx ->
      val trees = ctx.func(JavaFileScannerContext::getTree).tree()
      queryRule.createQuery(trees)
    }
  }
}
