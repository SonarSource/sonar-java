package org.sonar.java.checks;

import java.util.List;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

import static org.sonar.java.ast.api.JavaKeyword.THIS;

public final class InitializeSubclassFieldsBeforeSuperCheck extends FlexibleConstructorVisitor {


  @Override
  void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex) {
    if (constructorCallIndex < 0) {
      // Super called implicitly, so we disable the rule to avoid false positives.
      return;
    }
    MethodTree superMethod;
    if (body.get(constructorCallIndex) instanceof ExpressionStatementTree esTree
      && esTree.expression() instanceof MethodInvocationTree miTree
      && (superMethod = miTree.methodSymbol().declaration()) != null
    ) {
      AssignmentsUsedInSuperCheck assignmentsUsedInSuperCheck = new AssignmentsUsedInSuperCheck(superMethod);
      body.subList(constructorCallIndex + 1, body.size()).forEach(statement -> statement.accept(assignmentsUsedInSuperCheck));
    }
  }

  private static boolean isFieldAssignment(AssignmentExpressionTree tree) {
    return tree.variable() instanceof MemberSelectExpressionTree mseTree
      && mseTree.expression() instanceof IdentifierTree idTree
      && THIS.getValue().equals(idTree.name());
  }

  private static boolean isFieldUsedInMethod(MethodTree mt, Symbol symbol) {
    SymbolUsedCheck symbolUsedCheck = new SymbolUsedCheck(symbol);
    var methodBlock = mt.block();
    if (methodBlock == null) {
      // the function is not implemented (abstract method, interface method...),
      // so we consider that any symbol could be used in it.
      return true;
    }
    methodBlock.body().forEach(statement -> statement.accept(symbolUsedCheck));
    return symbolUsedCheck.isSymbolUsed();
  }

  private final class AssignmentsUsedInSuperCheck extends StatementVisitor {
    private final MethodTree superMethod;

    private AssignmentsUsedInSuperCheck(MethodTree superMethod) {
      this.superMethod = superMethod;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (
        isFieldAssignment(tree)
          && tree.variable() instanceof MemberSelectExpressionTree mseTree
          && isFieldUsedInMethod(superMethod, mseTree.identifier().symbol())
      ) {
        reportIssue(tree, "Initialize subclass fields before calling super constructor.");
      }
      super.visitAssignmentExpression(tree);
    }
  }

  private static class SymbolUsedCheck extends StatementVisitor {
    private boolean symbolUsed = false;
    private final Symbol symbol;

    private SymbolUsedCheck(Symbol symbol) {
      this.symbol = symbol;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      symbolUsed |= tree.symbol() == symbol;
      super.visitIdentifier(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      var methodDeclaration = tree.methodSymbol().declaration();
      if (methodDeclaration != null) {
        symbolUsed |= isFieldUsedInMethod(methodDeclaration, symbol);
      }
      super.visitMethodInvocation(tree);
    }

    public boolean isSymbolUsed() {
      return symbolUsed;
    }
  }
}
