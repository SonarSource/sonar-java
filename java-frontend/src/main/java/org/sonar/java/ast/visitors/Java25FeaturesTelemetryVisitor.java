package org.sonar.java.ast.visitors;

import java.util.List;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class Java25FeaturesTelemetryVisitor extends SubscriptionVisitor {
  private final Telemetry telemetry;

  public Java25FeaturesTelemetryVisitor(Telemetry telemetry) {
    this.telemetry = telemetry;
  }


  public void scan(Tree ast) {
    // public wrapper for protected scanTree method
    scanTree(ast);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONSTRUCTOR, Tree.Kind.IMPORT, Tree.Kind.IMPLICIT_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ImportTree importTree && importTree.isModule()) {
      aggregate(TelemetryKey.JAVA_FEATURE_MODULE_IMPORT);
    } else if (tree instanceof ClassTree) {
      aggregate(TelemetryKey.JAVA_FEATURE_COMPACT_SOURCE_FILES);
    } else if (
      tree instanceof MethodTree methodTree
        && methodTree.block().body().stream()
        // if the 1st statement of the constructor is a call to another constructor, then the flexible constructor body feature is not used.
        .skip(1)
        // if any other statement is a call to another constructor, then the flexible constructor body feature is used.
        .anyMatch(this::isSuperInvocation)
    ) {
      aggregate(TelemetryKey.JAVA_FEATURE_FLEXIBLE_CONSTRUCTOR_BODY);
    }
  }

  private void aggregate(TelemetryKey key) {
    // MODIFY HERE TO CHANGE AGGREGATION LOGIC (for example, to aggregate as a flag instead of a counter)
    telemetry.aggregateAsCounter(key, 1);
  }

  private boolean isSuperInvocation(StatementTree statement) {
    return statement instanceof ExpressionStatementTree expressionStatementTree
      && expressionStatementTree.expression() instanceof MethodInvocationTree methodInvocationTree
      && methodInvocationTree.methodSelect() instanceof IdentifierTree identifierTree
      && ExpressionUtils.isThisOrSuper(identifierTree.name());
  }

}

