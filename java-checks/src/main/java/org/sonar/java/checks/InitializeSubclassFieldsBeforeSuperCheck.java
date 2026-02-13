package org.sonar.java.checks;

import java.util.List;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

public final class InitializeSubclassFieldsBeforeSuperCheck extends FlexibleConstructorVisitor {

  private final StatementVisitor statementVisitor = new StatementVisitor() {
    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (isFieldAssignment(tree)) {
        reportIssue(tree, "Initialize subclass fields before calling super constructor.");
      }
      super.visitAssignmentExpression(tree);
    }
  };

  @Override
  void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex) {
    if (constructorCallIndex < 0) {
      // Super called implicitly, so we disable the rule to avoid false positives.
      return;
    }
    body.subList(constructorCallIndex + 1, body.size()).forEach(statement -> statement.accept(statementVisitor));
  }

  private static boolean isFieldAssignment(AssignmentExpressionTree tree) {
    return tree.variable() instanceof MemberSelectExpressionTree mseTree
      && mseTree.expression() instanceof IdentifierTree idTree
      && "this".equals(idTree.name());
  }
}
