/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
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
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S6201")
public class InstanceOfPatternMatchingCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IF_STATEMENT, Tree.Kind.FOR_STATEMENT, Tree.Kind.WHILE_STATEMENT,
      Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR, Tree.Kind.CONDITIONAL_EXPRESSION);
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
        handleConditional(ifStatement.condition(), ifStatement.thenStatement(), ifStatement.elseStatement());
        break;
      case FOR_STATEMENT:
        ForStatementTree forStatement = (ForStatementTree) tree;
        if (forStatement.condition() != null) {
          // Technically a negated instanceof inside a for- or while-condition should make us look for casts that
          // come after the loop, but for that we'd need the CFG and that seems overkill for this rule.
          handleConditional(forStatement.condition(), forStatement.statement(), null);
        }
        break;
      case WHILE_STATEMENT:
        WhileStatementTree whileStatement = (WhileStatementTree) tree;
        handleConditional(whileStatement.condition(), whileStatement.statement(), null);
        break;
      case CONDITIONAL_AND:
        BinaryExpressionTree and = (BinaryExpressionTree) tree;
        handleConditional(and.leftOperand(), and.rightOperand(), null);
        break;
      case CONDITIONAL_OR:
        BinaryExpressionTree or = (BinaryExpressionTree) tree;
        handleConditional(or.leftOperand(), null, or.rightOperand());
        break;
      default: // CONDITIONAL_EXPRESSION
        ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
        handleConditional(conditional.condition(), conditional.trueExpression(), conditional.falseExpression());
        break;
    }
  }

  private void handleConditional(ExpressionTree condition, @Nullable Tree thenBody, @Nullable Tree elseBody) {
    findInstanceOf(condition).ifPresent(instanceOf -> {
      if (!instanceOf.negated && thenBody != null) {
        thenBody.accept(new BodyVisitor(instanceOf.tree));
      } else if (instanceOf.negated && elseBody != null) {
        elseBody.accept(new BodyVisitor(instanceOf.tree));
      }
    });
  }

  // Note: If a condition contains multiple instanceof checks, we'll only return (and thus only check) the first one.
  // The number of FNs resulting from that should be small and in the case that's probably the most common
  // (`if (x instanceof X && y instanceof Y) { X x = (X) x; Y y = (Y) y;}`), we'll still find the second issue after the
  // first has been fixed.
  private static Optional<InstanceOfInfo> findInstanceOf(ExpressionTree condition) {
    return findInstanceOf(condition, false);
  }

  private static Optional<InstanceOfInfo> findInstanceOf(ExpressionTree condition, boolean negated) {
    condition = ExpressionUtils.skipParentheses(condition);
    switch (condition.kind()) {
      case INSTANCE_OF:
        return Optional.of(new InstanceOfInfo((InstanceOfTree) condition, negated));
      case CONDITIONAL_AND:
        // If the condition is part of a negated AND, it won't dominate the else case of the if because only one of the
        // operands of the AND would have to be
        if (negated) return Optional.empty();
        return findInstanceOfInBinaryExpression(condition, negated);
      case CONDITIONAL_OR:
        if (!negated) return Optional.empty();
        return findInstanceOfInBinaryExpression(condition, negated);
      case LOGICAL_COMPLEMENT:
        return findInstanceOf(((UnaryExpressionTree) condition).expression(), !negated);
      default:
        return Optional.empty();
    }
  }

  private static Optional<InstanceOfInfo> findInstanceOfInBinaryExpression(ExpressionTree condition, boolean negated) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) condition;
    Optional<InstanceOfInfo> leftResult = findInstanceOf(binaryExpression.leftOperand(), negated);
    if (leftResult.isPresent()) return leftResult;
    return findInstanceOf(binaryExpression.rightOperand(), negated);
  }

  private static class InstanceOfInfo {
    InstanceOfTree tree;
    boolean negated;

    public InstanceOfInfo(InstanceOfTree tree, boolean negated) {
      this.tree = tree;
      this.negated = negated;
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
