/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S3518")
public class DivisionByZeroCheck extends SECheck {

  private static final ExceptionalYieldChecker EXCEPTIONAL_YIELD_CHECKER = new ExceptionalYieldChecker(
    "A division by zero will occur when invoking method %s().");

  private enum ZeroConstraint implements Constraint {
    ZERO,
    NON_ZERO;

    @Override
    public String valueAsString() {
      if (this == ZeroConstraint.ZERO) {
        return "zero";
      }
      return "non-zero";
    }

    @Override
    public boolean isValidWith(@Nullable Constraint constraint) {
      return constraint == null || this == constraint;
    }

    @Override
    public Constraint inverse() {
      if (this == ZERO) {
        return NON_ZERO;
      }
      // inverse of NON_ZERO is unset : for a != b if a is nonzero then nothing is known for b (can be either zero or non-zero)
      return null;
    }
  }

  /**
   * This SV is only used to hold the constraint to set alongside an initial object constraint in the PostStatementVisitor
   */
  private static class DeferredConstraintHolderSV extends SymbolicValue {

    @Nullable
    private final ZeroConstraint deferredConstraint;

    DeferredConstraintHolderSV(int id, @Nullable ZeroConstraint deferredConstraint) {
      super(id);
      this.deferredConstraint = deferredConstraint;
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {
    private final ConstraintManager constraintManager;
    private final CheckerContext context;

    PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
      this.constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      List<SymbolicValue> symbolicValues;
      SymbolicValue var;
      SymbolicValue expr;
      if (ExpressionUtils.isSimpleAssignment(tree)) {
        var = programState.getValue(((IdentifierTree) ExpressionUtils.skipParentheses(tree.variable())).symbol());
        expr = programState.peekValue();
      } else {
        symbolicValues = programState.peekValues(2);
        var = symbolicValues.get(1);
        expr = symbolicValues.get(0);
      }

      checkExpression(tree, var, expr);
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      List<SymbolicValue> symbolicValues;
      switch (tree.kind()) {
        case MULTIPLY:
        case PLUS:
        case MINUS:
        case DIVIDE:
        case REMAINDER:
          symbolicValues = programState.peekValues(2);
          checkExpression(tree, symbolicValues.get(1), symbolicValues.get(0));
          break;
        case GREATER_THAN:
        case GREATER_THAN_OR_EQUAL_TO:
        case LESS_THAN:
        case LESS_THAN_OR_EQUAL_TO:
          symbolicValues = programState.peekValues(2);
          removeZeroConstraint(symbolicValues.get(1));
          removeZeroConstraint(symbolicValues.get(0));
          break;
        default:
          // do nothing
      }
    }

    private void removeZeroConstraint(SymbolicValue sv) {
      programState = programState.removeConstraintsOnDomain(sv, ZeroConstraint.class);
    }

    private void checkExpression(Tree tree, SymbolicValue leftOp, SymbolicValue rightOp) {
      switch (tree.kind()) {
        case MULTIPLY:
        case MULTIPLY_ASSIGNMENT:
          handleMultiply(leftOp, rightOp);
          break;
        case PLUS:
        case PLUS_ASSIGNMENT:
        case MINUS:
        case MINUS_ASSIGNMENT:
          handlePlusMinus(leftOp, rightOp);
          break;
        case DIVIDE:
        case DIVIDE_ASSIGNMENT:
        case REMAINDER:
        case REMAINDER_ASSIGNMENT:
          handleDivide(tree, leftOp, rightOp);
          break;
        default:
          // can not be reached
      }
    }

    private boolean isZero(SymbolicValue symbolicValue) {
      return hasConstraint(symbolicValue, ZeroConstraint.ZERO);
    }

    private boolean isNonZero(SymbolicValue symbolicValue) {
      return hasConstraint(symbolicValue, ZeroConstraint.NON_ZERO);
    }

    private boolean hasNoConstraint(SymbolicValue symbolicValue) {
      return hasConstraint(symbolicValue, null);
    }

    private boolean hasConstraint(SymbolicValue symbolicValue, ZeroConstraint constraint) {
      return programState.getConstraint(symbolicValue, ZeroConstraint.class) == constraint;
    }

    private void handleMultiply(SymbolicValue left, SymbolicValue right) {
      boolean leftIsZero = isZero(left);
      if (leftIsZero || isZero(right)) {
        reuseSymbolicValue(leftIsZero ? left : right);
      } else if (isNonZero(left) && isNonZero(right)) {
        deferConstraint(ZeroConstraint.NON_ZERO);
      }
    }

    private void handlePlusMinus(SymbolicValue left, SymbolicValue right) {
      boolean leftIsZero = isZero(left);
      if (leftIsZero || isZero(right)) {
        reuseSymbolicValue(leftIsZero ? right : left);
      }
    }

    private void handleDivide(Tree tree, SymbolicValue leftOp, SymbolicValue rightOp) {
      if (isZero(rightOp)) {
        context.addExceptionalYield(rightOp, programState, "java.lang.ArithmeticException", DivisionByZeroCheck.this);
        reportIssue(tree, rightOp);
        // interrupt exploration
        programState = null;
      } else if (isZero(leftOp)) {
        reuseSymbolicValue(leftOp);
      } else if (isNonZero(leftOp) && isNonZero(rightOp)) {
        deferConstraint(tree.is(Tree.Kind.DIVIDE, Tree.Kind.DIVIDE_ASSIGNMENT) ? ZeroConstraint.NON_ZERO : null);
      } else if (hasNoConstraint(rightOp)) {
        ProgramState exceptionalState = programState
          .addConstraint(rightOp, ZeroConstraint.ZERO)
          // FIXME SONARJAVA-2125 - we should not have to add the NOT_NULL constraint for primitive types
          .addConstraint(rightOp, ObjectConstraint.NOT_NULL);
        context.addExceptionalYield(rightOp, exceptionalState, "java.lang.ArithmeticException", DivisionByZeroCheck.this);
      }
    }

    private void deferConstraint(@Nullable ZeroConstraint constraint) {
      constraintManager.setValueFactory(id -> new DeferredConstraintHolderSV(id, constraint));
    }

    private void reuseSymbolicValue(SymbolicValue sv) {
      constraintManager.setValueFactory(id -> new DeferredConstraintHolderSV(id, programState.getConstraint(sv, ZeroConstraint.class)) {
        @Override
        public SymbolicValue wrappedValue() {
          return sv.wrappedValue();
        }
      });
    }

    private void reportIssue(Tree tree, SymbolicValue denominator) {
      ExpressionTree expression = getDenominator(tree);
      String operation = tree.is(Tree.Kind.REMAINDER, Tree.Kind.REMAINDER_ASSIGNMENT) ? "modulation" : "division";
      String expressionName;
      String flowMessage;
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        String name = ((IdentifierTree) expression).name();
        expressionName = "'" + name + "'";
        flowMessage = expressionName + " is divided by zero";
      } else {
        expressionName = "this expression";
        flowMessage = "this expression contains division by zero";
      }
      List<Class<? extends Constraint>> domains = Lists.newArrayList(ZeroConstraint.class);
      Set<List<JavaFileScannerContext.Location>> flows = FlowComputation.flow(context.getNode(), denominator, domains).stream()
        .map(f -> ImmutableList.<JavaFileScannerContext.Location>builder()
          .add(new JavaFileScannerContext.Location(flowMessage, tree))
          .addAll(f)
          .build())
        .collect(Collectors.toSet());
      context.reportIssue(expression, DivisionByZeroCheck.this, "Make sure " + expressionName + " can't be zero before doing this " + operation + ".",
        flows);
    }

    private ExpressionTree getDenominator(Tree tree) {
      return tree.is(Tree.Kind.DIVIDE, Tree.Kind.REMAINDER) ? ((BinaryExpressionTree) tree).rightOperand() : ((AssignmentExpressionTree) tree).expression();
    }

    @Override
    public void visitTypeCast(TypeCastTree tree) {
      Type type = tree.type().symbolType();
      if (type.isPrimitive()) {
        SymbolicValue sv = programState.peekValue();
        if (isZero(sv)) {
          reuseSymbolicValue(sv);
        } else if (isNonZero(sv)) {
          deferConstraint(ZeroConstraint.NON_ZERO);
        }
      }
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      if (!tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        SymbolicValue sv = programState.peekValue();
        if (isZero(sv)) {
          if (tree.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
            reuseSymbolicValue(sv);
          } else {
            deferConstraint(ZeroConstraint.NON_ZERO);
          }
        } else {
          deferConstraint(null);
        }
      }
    }
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitLiteral(LiteralTree tree) {
      String value = tree.value();
      SymbolicValue sv = programState.peekValue();
      if (tree.is(Tree.Kind.CHAR_LITERAL) && isNullCharacter(value)) {
        addZeroConstraint(sv, ZeroConstraint.ZERO);
      } else if (tree.is(Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL)) {
        addZeroConstraint(sv, isNumberZero(value) ? ZeroConstraint.ZERO : ZeroConstraint.NON_ZERO);
      }
    }

    private static boolean isNumberZero(String literalValue) {
      return !(literalValue.matches("(.)*[1-9]+(.)*") || literalValue.matches("(0x|0X){1}(.)*[1-9a-fA-F]+(.)*") || literalValue.matches("(0b|0B){1}(.)*[1]+(.)*"));
    }

    private static boolean isNullCharacter(String literalValue) {
      return "'\\0'".equals(literalValue) || "'\\u0000'".equals(literalValue);
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      checkDeferredConstraint();
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkDeferredConstraint();
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      checkDeferredConstraint();
    }

    @Override
    public void visitTypeCast(TypeCastTree tree) {
      checkDeferredConstraint();
    }

    private void checkDeferredConstraint() {
      SymbolicValue sv = programState.peekValue();
      if (sv instanceof DeferredConstraintHolderSV) {
        addZeroConstraint(sv, ((DeferredConstraintHolderSV) sv).deferredConstraint);
      }
    }

    private void addZeroConstraint(SymbolicValue sv, @Nullable ZeroConstraint zeroConstraint) {
      if(zeroConstraint == null) {
        programState = programState.removeConstraintsOnDomain(sv, ZeroConstraint.class);
      } else {
        programState = programState.addConstraint(sv, zeroConstraint);
      }
    }
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    EXCEPTIONAL_YIELD_CHECKER.reportOnExceptionalYield(context.getNode(), this);
  }
}
