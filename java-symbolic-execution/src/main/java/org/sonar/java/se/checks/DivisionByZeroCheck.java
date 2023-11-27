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
package org.sonar.java.se.checks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S3518")
public class DivisionByZeroCheck extends SECheck {

  private static final String BIG_INTEGER = "java.math.BigInteger";
  private static final String BIG_DECIMAL = "java.math.BigDecimal";

  private static final ExceptionalYieldChecker EXCEPTIONAL_YIELD_CHECKER = new ExceptionalYieldChecker(
    "A division by zero will occur when invoking method \"%s()\".");

  private static final MethodMatchers.NameBuilder BIG_INTEGER_AND_DECIMAL = MethodMatchers.create()
    .ofTypes(BIG_INTEGER, BIG_DECIMAL);
  private static final MethodMatchers BIG_INT_DEC_VALUE_OF = BIG_INTEGER_AND_DECIMAL
    .names("valueOf").addParametersMatcher(MethodMatchers.ANY).build();
  private static final MethodMatchers BIG_INT_DEC_DIVIDE_REMAINDER = BIG_INTEGER_AND_DECIMAL
    .names("divide", "remainder", "divideAndRemainder").addParametersMatcher(MethodMatchers.ANY).build();
  private static final MethodMatchers BIG_INT_DEC_MULTIPLY = BIG_INTEGER_AND_DECIMAL
    .names("multiply").addParametersMatcher(MethodMatchers.ANY).build();
  private static final MethodMatchers BIG_INT_DEC_ADD_SUB = BIG_INTEGER_AND_DECIMAL
    .names("add", "subtract").addParametersMatcher(MethodMatchers.ANY).build();
  private static final MethodMatchers KEEPING_CONSTRAINTS_WITHOUT_PARAM = BIG_INTEGER_AND_DECIMAL
    .names("toBigInteger", "toBigIntegerExact", "abs", "byteValueExact", "byteValue", "byteValueExact", "shortValue", "shortValueExact",
      "doubleValue", "floatValue", "intValue", "intValueExact", "longValue", "longValueExact").addParametersMatcher().build();
  private static final MethodMatchers KEEPING_CONSTRAINTS_WITH_ONE_PARAM = BIG_INTEGER_AND_DECIMAL
    .names("pow", "round", "shiftRight", "shiftLeft").addParametersMatcher(MethodMatchers.ANY).build();

  private final Map<String, Boolean> zeroValuesCache = new HashMap<>();

  public enum ZeroConstraint implements Constraint {
    ZERO,
    NON_ZERO;

    @Override
    public boolean hasPreciseValue() {
      return this == ZERO;
    }

    @Override
    public String valueAsString() {
      if (this == ZERO) {
        return "zero";
      }
      return "non-zero";
    }

    @Override
    public boolean isValidWith(@Nullable Constraint constraint) {
      return constraint == null || this == constraint;
    }

    @Nullable
    @Override
    public Constraint copyOver(RelationalSymbolicValue.Kind kind) {
      switch (kind) {
        case EQUAL:
        case METHOD_EQUALS:
          return this;
        case LESS_THAN:
        case NOT_EQUAL:
        case NOT_METHOD_EQUALS:
          return inverse();
        default:
          return null;
      }
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

    DeferredConstraintHolderSV(@Nullable ZeroConstraint deferredConstraint) {
      this.deferredConstraint = deferredConstraint;
    }
  }

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    zeroValuesCache.clear();
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
      SymbolicValue variable;
      SymbolicValue expression;
      Symbol symbol;
      if (ExpressionUtils.isSimpleAssignment(tree)) {
        symbol = ExpressionUtils.extractIdentifier(tree).symbol();
        variable = programState.getValue(symbol);
        expression = programState.peekValue();
      } else {
        ProgramState.Pop unstackValue = programState.unstackValue(2);
        variable = unstackValue.values.get(1);
        expression = unstackValue.values.get(0);
        symbol = unstackValue.valuesAndSymbols.get(0).symbol();
      }

      checkExpression(tree, variable, expression, symbol);
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      switch (tree.kind()) {
        case MULTIPLY:
        case PLUS:
        case MINUS:
        case DIVIDE:
        case REMAINDER:
          ProgramState.Pop unstackValue = programState.unstackValue(2);
          checkExpression(tree, unstackValue.values.get(1), unstackValue.values.get(0), unstackValue.valuesAndSymbols.get(0).symbol());
          break;
        default:
          // do nothing
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (BIG_INT_DEC_DIVIDE_REMAINDER.matches(tree)) {
        ProgramState.SymbolicValueSymbol rightOp = programState.peekValueSymbol();
        handleDivide(tree, programState.peekValue(1), rightOp.symbolicValue(), rightOp.symbol());
      } else if (BIG_INT_DEC_ADD_SUB.matches(tree)) {
        handlePlusMinus(programState.peekValue(1), programState.peekValue(0));
      } else if (BIG_INT_DEC_MULTIPLY.matches(tree)) {
        handleMultiply(programState.peekValue(1), programState.peekValue(0));
      } else if (BIG_INT_DEC_VALUE_OF.matches(tree)) {
        ExpressionTree arg = tree.arguments().get(0);
        SymbolicValue sv = programState.peekValue(0);
        if (arg.is(Tree.Kind.IDENTIFIER) && isZero(sv)) {
          reuseSymbolicValue(sv);
        }
      } else if (KEEPING_CONSTRAINTS_WITHOUT_PARAM.matches(tree)) {
        reuseSymbolicValue(programState.peekValue(0));
      } else if (KEEPING_CONSTRAINTS_WITH_ONE_PARAM.matches(tree)) {
        reuseSymbolicValue(programState.peekValue(1));
      }
    }

    private void checkExpression(Tree tree, SymbolicValue leftOp, SymbolicValue rightOp, Symbol rightOpSymbol) {
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
          handleDivide(tree, leftOp, rightOp, rightOpSymbol);
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

    private void handleDivide(Tree tree, SymbolicValue leftOp, SymbolicValue rightOp, Symbol rightOpSymbol) {
      if (isZero(rightOp)) {
        context.addExceptionalYield(rightOp, programState, "java.lang.ArithmeticException", DivisionByZeroCheck.this);
        reportIssue(tree, rightOp, rightOpSymbol);
        // interrupt exploration
        programState = null;
      } else if (isZero(leftOp)) {
        reuseSymbolicValue(leftOp);
      } else if (isNonZero(leftOp) && isNonZero(rightOp)) {
        // result of 'integer' can be zero or non-zero, depending of operands (for instance: '1 / 2 == 0')
        deferConstraint(null);
      } else if (hasNoConstraint(rightOp)) {
        ProgramState exceptionalState = programState
          .addConstraint(rightOp, ZeroConstraint.ZERO)
          // FIXME SONARJAVA-2125 - we should not have to add the NOT_NULL constraint for primitive types
          .addConstraint(rightOp, ObjectConstraint.NOT_NULL);
        context.addExceptionalYield(rightOp, exceptionalState, "java.lang.ArithmeticException", DivisionByZeroCheck.this);
        programState = programState.addConstraintTransitively(rightOp, ZeroConstraint.NON_ZERO);
      }
    }

    private void deferConstraint(@Nullable ZeroConstraint constraint) {
      constraintManager.setValueFactory(() -> new DeferredConstraintHolderSV(constraint));
    }

    private void reuseSymbolicValue(SymbolicValue sv) {
      constraintManager.setValueFactory(() -> new DeferredConstraintHolderSV(programState.getConstraint(sv, ZeroConstraint.class)) {
        @Override
        public SymbolicValue wrappedValue() {
          return sv.wrappedValue();
        }
      });
    }

    private void reportIssue(Tree tree, SymbolicValue denominator, Symbol denominatorSymbol) {
      ExpressionTree expression = getDenominator(tree);
      String operation = tree.is(Tree.Kind.REMAINDER, Tree.Kind.REMAINDER_ASSIGNMENT) ? "modulation" : "division";
      String expressionName = expression.is(Tree.Kind.IDENTIFIER) ? ("\"" + ((IdentifierTree) expression).name() + "\"") : "this expression";
      List<Class<? extends Constraint>> domains = Collections.singletonList(ZeroConstraint.class);
      Set<Flow> flows = FlowComputation.flow(context.getNode(), denominator, domains, denominatorSymbol, FlowComputation.MAX_REPORTED_FLOWS).stream()
        .filter(f -> !f.isEmpty())
        .map(f -> Flow.builder()
          .add(new JavaFileScannerContext.Location("Division by zero.", tree))
          .addAll(f)
          .build())
        .collect(Collectors.toSet());
      context.reportIssue(expression, DivisionByZeroCheck.this, "Make sure " + expressionName + " can't be zero before doing this " + operation + ".",
        flows);
    }

    private ExpressionTree getDenominator(Tree tree) {
      if (tree.is(Tree.Kind.DIVIDE, Tree.Kind.REMAINDER)) {
        return ((BinaryExpressionTree) tree).rightOperand();
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        return ((MethodInvocationTree) tree).arguments().get(0);
      } else {
        return ((AssignmentExpressionTree) tree).expression();
      }
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
    PostStatementVisitor visitor = new PostStatementVisitor(context, zeroValuesCache);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    private final Map<String, Boolean> zeroValuesCache;

    PostStatementVisitor(CheckerContext context, Map<String, Boolean> zeroValuesCache) {
      super(context.getState());
      this.zeroValuesCache = zeroValuesCache;
    }

    @Override
    public void visitIdentifier(IdentifierTree identifier) {
      var symbol = identifier.symbol();
      if (!(symbol instanceof Symbol.VariableSymbol)) {
        return;
      }
      SymbolicValue sv = programState.getValue(symbol);
      if (sv == null) {
        return;
      }
      if (programState.getConstraint(sv, ZeroConstraint.class) != null) {
        // no need to reassign
        return;
      }
      Type type = identifier.symbolType();
      if (type.isPrimitive() || type.isPrimitiveWrapper()) {
        ((Symbol.VariableSymbol) symbol).constantValue()
          .filter(Number.class::isInstance)
          .map(Number.class::cast)
          .ifPresent(num -> {
            if (isZero(num, symbol.type().fullyQualifiedName())) {
              addZeroConstraint(sv, ZeroConstraint.ZERO);
            } else {
              addZeroConstraint(sv, ZeroConstraint.NON_ZERO);
            }
          });
      }
    }

    private static boolean isZero(Number number, String type) {
      switch (type) {
        case "char":
          return number.intValue() == 0;
        case "byte":
          return number.byteValue() == 0;
        case "short":
          return number.shortValue() == 0;
        case "int":
          return number.intValue() == 0;
        case "long":
          return number.longValue() == 0L;
        case "double":
          return number.doubleValue() == 0.0;
        case "float":
          return number.floatValue() == 0.0;
        default:
          // should not be possible
          return false;
      }
    }

    @Override
    public void visitLiteral(LiteralTree tree) {
      handleLiteral(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (BIG_INT_DEC_VALUE_OF.matches(tree)) {
        ExpressionTree arg = tree.arguments().get(0);
        if (arg instanceof LiteralTree) {
          handleLiteral(((LiteralTree) arg));
          return;
        }
      }
      checkDeferredConstraint();
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      IdentifierTree id = tree.identifier();
      Type idType = id.symbolType();
      SymbolicValue sv = programState.peekValue();
      if (sv != null && (idType.is(BIG_INTEGER) || idType.is(BIG_DECIMAL))) {
        if ("ZERO".equals(id.name())) {
          addZeroConstraint(sv, ZeroConstraint.ZERO);
        } else {
          addZeroConstraint(sv, ZeroConstraint.NON_ZERO);
        }
      }

      super.visitMemberSelectExpression(tree);
    }

    private void handleLiteral(LiteralTree tree) {
      String value = tree.value();
      SymbolicValue sv = programState.peekValue();
      if (tree.is(Tree.Kind.CHAR_LITERAL) && isNullCharacter(value)) {
        addZeroConstraint(sv, ZeroConstraint.ZERO);
      } else if (tree.is(Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL)) {
        addZeroConstraint(sv, checkZeroLiteral(tree) ? ZeroConstraint.ZERO : ZeroConstraint.NON_ZERO);
      }
    }

    private boolean checkZeroLiteral(LiteralTree literalTree) {
      String value = literalTree.value();
      return zeroValuesCache.computeIfAbsent(value, v -> isZeroLiteral(literalTree, value));
    }

    /**
     * This method is used to check whether the string value of a given number literal (int, long, float or double)
     * is zero. As far as this method is considered to be on the hot paths, it requires some performant solution
     * with early returns rather then computing the value of the literal and than checking it, because that will
     * require a lot of boxing and unboxing operations.
     *
     * asConstant() method is not applicable here because it requires boxing and unboxing to get the value and then
     * to check it.
     **/
    private static boolean isZeroLiteral(LiteralTree literalTree, String value) {
      if (value.length() == 1) {
        return "0".equals(value);
      }
      int startIndex = 0;
      int endIndex = value.length() - 1;
      switch (literalTree.kind()) {
        case LONG_LITERAL:
          endIndex = value.length() - 2;
        case INT_LITERAL:
          if (value.charAt(0) == '0' && (value.charAt(1) == 'x' || value.charAt(1) == 'X' ||
            value.charAt(1) == 'b' || value.charAt(1) == 'B')) {
            startIndex = 2;
          }
          break;
        case FLOAT_LITERAL:
          endIndex = value.length() - 2;
          break;
        case DOUBLE_LITERAL:
          if (value.charAt(endIndex) == 'd' || value.charAt(endIndex) == 'D') {
            endIndex = value.length() - 2;
          }
          break;
      }
      return isNumberZero(value, startIndex, endIndex);
    }


    private static boolean isNumberZero(String literalValue, int startIndex, int endIndex) {
      for (int i = startIndex; i <= endIndex; ++i) {
        if (literalValue.charAt(i) != '_' && literalValue.charAt(i) != '0' && literalValue.charAt(i) != '.') {
          return false;
        }
      }
      return true;
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
      if (zeroConstraint == null) {
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
