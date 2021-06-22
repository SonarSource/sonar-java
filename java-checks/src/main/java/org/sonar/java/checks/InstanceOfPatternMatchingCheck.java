/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6201")
public class InstanceOfPatternMatchingCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IF_STATEMENT, Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava16Compatible();
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case IF_STATEMENT:
        IfStatementTree ifStatement = (IfStatementTree) tree;
        handleConditional(ifStatement.condition(), ifStatement.thenStatement());
        break;
      case CONDITIONAL_AND:
        BinaryExpressionTree and = (BinaryExpressionTree) tree;
        handleConditional(and.leftOperand(), and.rightOperand());
        break;
      default: // CONDITIONAL_EXPRESSION
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
        handleConditional(conditional.condition(), conditional.trueExpression());
        break;
    }
  }

  private void handleConditional(ExpressionTree condition, Tree body) {
    findInstanceOf(condition).ifPresent(instanceOf ->
      body.accept(new BodyVisitor(instanceOf))
    );
  }

  private static Optional<InstanceOfTree> findInstanceOf(ExpressionTree condition) {
    condition = ExpressionUtils.skipParentheses(condition);
    switch (condition.kind()) {
      case INSTANCE_OF:
        return Optional.of((InstanceOfTree) condition);
      case CONDITIONAL_AND:
        BinaryExpressionTree and = (BinaryExpressionTree) condition;
        Optional<InstanceOfTree> leftResult = findInstanceOf(and.leftOperand());
        if (leftResult.isPresent()) return leftResult;
        return findInstanceOf(and.rightOperand());
      default:
        return Optional.empty();
    }
  }

  private class BodyVisitor extends BaseTreeVisitor {
    InstanceOfTree instanceOf;

    BodyVisitor(InstanceOfTree instanceOf) {
      this.instanceOf = instanceOf;
    }

    @Override
    public void visitVariable(VariableTree tree) {
      Type type = tree.type().symbolType();
      if (!type.isUnknown() && type.equals(instanceOf.type().symbolType())) {
        ExpressionTree init = tree.initializer();
        if (init != null && init.is(Tree.Kind.TYPE_CAST)
          && SyntacticEquivalence.areEquivalentIncludingSameVariables(((TypeCastTree) init).expression(), instanceOf.expression())) {
          report(instanceOf, init, tree.simpleName().name());
          return;
        }
      }
      super.visitVariable(tree);
    }

    @Override
    public void visitTypeCast(TypeCastTree tree) {
      Type type = tree.symbolType();
      if (!type.isUnknown() && type.equals(instanceOf.type().symbolType())
        && SyntacticEquivalence.areEquivalentIncludingSameVariables(tree.expression(), instanceOf.expression())) {
        report(instanceOf, tree, tree.type().symbolType().name().toLowerCase(Locale.ROOT));
      }
    }

    private void report(InstanceOfTree instanceOf, ExpressionTree cast, String name) {
      String type = instanceOf.type().symbolType().name();
      String message = String.format("Replace this instanceof check and cast with 'instanceof %s %s'", type, name);
      JavaFileScannerContext.Location secondary = new JavaFileScannerContext.Location("Location of the cast", cast);
      reportIssue(instanceOf, message, Collections.singletonList(secondary), null);
    }
  }
}
