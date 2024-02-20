/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;

@Rule(key = "S2259")
public class NullDereferenceCheck extends SECheck {

  private static final ExceptionalYieldChecker EXCEPTIONAL_YIELD_CHECKER = new ExceptionalYieldChecker(
    "\"NullPointerException\" will be thrown when invoking method \"%s()\".");

  private static final String JAVA_LANG_NPE = "java.lang.NullPointerException";
  private static final MethodMatchers OPTIONAL_OR_ELSE_GET_MATCHER = MethodMatchers.create().ofTypes("java.util.Optional").names("orElseGet")
    .addParametersMatcher("java.util.function.Supplier").build();

  private static final Set<String> BOXED_TYPES = Set.of(
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Float",
    "java.lang.Double",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Short"
  );

  private static class NullDereferenceIssue {
    final ExplodedGraph.Node node;
    final SymbolicValue symbolicValue;
    final Tree tree;

    private NullDereferenceIssue(ExplodedGraph.Node node, SymbolicValue symbolicValue, Tree tree) {
      this.node = node;
      this.symbolicValue = symbolicValue;
      this.tree = tree;
    }
  }

  private Deque<Set<NullDereferenceIssue>> detectedIssues = new ArrayDeque<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    detectedIssues.clear();
  }

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    detectedIssues.push(new HashSet<>());
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    SymbolicValue peekValue = context.getState().peekValue();
    if (peekValue == null) {
      // stack is empty, nothing to do.
      return context.getState();
    }
    switch (syntaxNode.kind()) {
      case METHOD_INVOCATION:
        MethodInvocationTree methodInvocation = (MethodInvocationTree) syntaxNode;
        ExpressionTree methodSelect = methodInvocation.methodSelect();
        ProgramState ps = context.getState();
        if (OPTIONAL_OR_ELSE_GET_MATCHER.matches(methodInvocation)) {
          ps = checkConstraint(context, methodInvocation.arguments().get(0), peekValue);
          if (ps == null) {
            return ps;
          }
        }
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          SymbolicValue dereferencedSV = context.getState().peekValue(methodInvocation.arguments().size());
          return checkConstraint(ps, context, methodSelect, dereferencedSV);
        }
        break;
      case ARRAY_ACCESS_EXPRESSION:
        Tree arrayAccessNode = ((ArrayAccessExpressionTree) syntaxNode).expression();
        SymbolicValue arrayAccessSV = context.getState().peekValue(1);
        return checkConstraint(context, arrayAccessNode, arrayAccessSV);
      case MEMBER_SELECT:
        return checkMemberSelect(context, (MemberSelectExpressionTree) syntaxNode, peekValue);
      case SYNCHRONIZED_STATEMENT:
        return checkConstraint(context, syntaxNode, peekValue);
      case VARIABLE:
        var variableTree = (VariableTree) syntaxNode;
        extractUnboxingExpression(variableTree).ifPresent(expression ->
          checkUnboxingConstraint(context, expression, peekValue)
        );
        break;
      case YIELD_STATEMENT:
        inspectYieldStatement(context, (YieldStatementTree) syntaxNode, peekValue);
        break;
      case RETURN_STATEMENT:
        inspectReturnStatement(context, (ReturnStatementTree) syntaxNode, peekValue);
        break;
      default:
        if (syntaxNode instanceof AssignmentExpressionTree assignmentExpressionTree) {
          inspectAssignment(context, assignmentExpressionTree, peekValue);
        } else if (syntaxNode instanceof BinaryExpressionTree binaryExpressionTree) {
          inspectBinaryExpression(context, binaryExpressionTree, peekValue);
        } else if (syntaxNode instanceof UnaryExpressionTree unaryOperation) {
          extractUnboxingExpression(unaryOperation).ifPresent(expression ->
            checkUnboxingConstraint(context, expression, peekValue)
          );
        }
    }
    return context.getState();
  }

  private void inspectYieldStatement(CheckerContext context, YieldStatementTree yieldStatementTree, SymbolicValue peekValue) {
    inspectReturnExpression(context, yieldStatementTree.expression(), peekValue);
  }

  private void inspectReturnStatement(CheckerContext context, ReturnStatementTree returnExpression, SymbolicValue peekValue) {
    ExpressionTree expression = returnExpression.expression();
    if (expression != null) {
      inspectReturnExpression(context, expression, peekValue);
    }
  }

  private void inspectReturnExpression(CheckerContext context, ExpressionTree returnedExpression, SymbolicValue peekValue) {
    if (!BOXED_TYPES.contains(returnedExpression.symbolType().fullyQualifiedName())) {
      return;
    }
    getParentScope(returnedExpression).ifPresent(tree -> {
      Type type;
      if (tree.is(Tree.Kind.METHOD)) {
        type = ((MethodTree) tree).returnType().symbolType();
      } else {
        type = ((SwitchExpressionTree) tree).symbolType();
      }
      if (type.isPrimitive()) {
        checkUnboxingConstraint(context, returnedExpression, peekValue);
      }
    });
  }

  private static Optional<Tree> getParentScope(Tree tree) {
    Tree parent = tree;
    do {
      parent = parent.parent();
    } while (parent != null && !parent.is(Tree.Kind.METHOD, Tree.Kind.SWITCH_EXPRESSION));
    return Optional.ofNullable(parent);
  }

  private void inspectAssignment(CheckerContext context, AssignmentExpressionTree syntaxNode, SymbolicValue peekValue) {
    var assignment = syntaxNode;
    extractUnboxingExpression(assignment).ifPresent(expression -> checkUnboxingConstraint(context, expression, peekValue));
  }

  private void inspectBinaryExpression(CheckerContext context, BinaryExpressionTree syntaxNode, SymbolicValue peekValue) {
    var binaryOperation = syntaxNode;
    extractUnboxingExpression(binaryOperation).ifPresent(expression -> checkUnboxingConstraint(context, expression, peekValue));
  }

  private ProgramState checkUnboxingConstraint(CheckerContext context, ExpressionTree expression, SymbolicValue defaultValue) {
    SymbolicValue constraintValue = defaultValue;
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      SymbolicValue value = context.getState().getValue(symbol);
      if (value != null) {
        constraintValue = value;
      }
    }
    return checkConstraint(context, expression, constraintValue);
  }

  private static Optional<ExpressionTree> extractUnboxingExpression(VariableTree variableTree) {
    Type receivingType = variableTree.symbol().type();
    if (!receivingType.isPrimitive()) {
      return Optional.empty();
    }
    ExpressionTree initializer = variableTree.initializer();
    if (initializer == null) {
      return Optional.empty();
    }

    Type type = initializer.symbolType();
    if (type.isPrimitive()) {
      return Optional.empty();
    }
    return Optional.of(initializer);
  }

  private static Optional<ExpressionTree> extractUnboxingExpression(AssignmentExpressionTree assignmentTree) {
    Optional<ExpressionTree> boxedOperand = selectBoxedTypeExpression(
      assignmentTree.variable(),
      assignmentTree.expression()
    );
    // If the assignment simply overwrites the boxed operand using a primitive, then there will be no unboxing and we can return empty
    if (assignmentTree.kind().equals(Tree.Kind.ASSIGNMENT) &&
      boxedOperand.isPresent() &&
      boxedOperand.get().equals(assignmentTree.variable())) {
      return Optional.empty();
    }
    return boxedOperand;
  }

  private static Optional<ExpressionTree> extractUnboxingExpression(BinaryExpressionTree expressionTree) {
    return selectBoxedTypeExpression(
      expressionTree.leftOperand(),
      expressionTree.rightOperand()
    );
  }

  private static Optional<ExpressionTree> selectBoxedTypeExpression(ExpressionTree left, ExpressionTree right) {
    var leftType = left.symbolType();
    var rightType = right.symbolType();
    if (left.is(Tree.Kind.NULL_LITERAL) || right.is(Tree.Kind.NULL_LITERAL) ||
      (leftType.isPrimitive() && rightType.isPrimitive()) ||
      (!leftType.isPrimitive() && !rightType.isPrimitive())) {
      return Optional.empty();
    }
    if (leftType.isPrimitive() && BOXED_TYPES.contains(rightType.fullyQualifiedName())) {
      return Optional.of(right);
    }
    // If we reached this far, then the expression on the right must be a primitive
    if (BOXED_TYPES.contains(leftType.fullyQualifiedName())) {
      return Optional.of(left);
    }
    return Optional.empty();
  }

  private static Optional<ExpressionTree> extractUnboxingExpression(UnaryExpressionTree tree) {
    ExpressionTree expression = tree.expression();
    Type type = expression.symbolType();
    if (BOXED_TYPES.contains(type.fullyQualifiedName())) {
      return Optional.of(expression);
    }
    return Optional.empty();
  }
  private ProgramState checkMemberSelect(CheckerContext context, MemberSelectExpressionTree mse, SymbolicValue currentVal) {
    if ("class".equals(mse.identifier().name())) {
      // expression ClassName.class won't raise NPE.
      return context.getState();
    }
    return checkConstraint(context, mse, currentVal);
  }

  private ProgramState checkConstraint(CheckerContext context, Tree syntaxNode, SymbolicValue currentVal) {
    return checkConstraint(context.getState(), context, syntaxNode, currentVal);
  }

  private ProgramState checkConstraint(ProgramState programState, CheckerContext context, Tree syntaxNode, SymbolicValue currentVal) {
    ObjectConstraint constraint = programState.getConstraint(currentVal, ObjectConstraint.class);
    if (constraint != null && constraint.isNull()) {
      NullDereferenceIssue issue = new NullDereferenceIssue(context.getNode(), currentVal, syntaxNode);
      detectedIssues.peek().add(issue);

      // we reported the issue and stopped the exploration, but we still need to create a yield for x-procedural calls
      context.addExceptionalYield(currentVal, programState, JAVA_LANG_NPE, this);
      return null;
    }
    constraint = programState.getConstraint(currentVal, ObjectConstraint.class);
    if (constraint == null) {
      // a NPE will be triggered if the current value would have been null
      context.addExceptionalYield(currentVal, programState.addConstraint(currentVal, ObjectConstraint.NULL), JAVA_LANG_NPE, this);

      // We dereferenced the target value for the member select, so we can assume it is not null when not already known
      return programState.addConstraint(currentVal, ObjectConstraint.NOT_NULL);
    }
    return programState;
  }

  private void reportIssue(SymbolicValue currentVal, Tree syntaxNode, ExplodedGraph.Node node) {
    String message = "A \"NullPointerException\" could be thrown; ";
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)
      && ((MemberSelectExpressionTree) syntaxNode).expression().is(Tree.Kind.METHOD_INVOCATION)) {
      message += "\"" + SyntaxTreeNameFinder.getName(syntaxNode) + "()\" can return null.";
    } else {
      message += "\"" + SyntaxTreeNameFinder.getName(syntaxNode) + "\" is nullable here.";
    }
    SymbolicValue val = null;
    if (!SymbolicValue.NULL_LITERAL.equals(currentVal)) {
      val = currentVal;
    }
    Symbol dereferencedSymbol = dereferencedSymbol(syntaxNode);
    Set<Flow> flows = FlowComputation.flow(node, val, Collections.singletonList(ObjectConstraint.class), dereferencedSymbol, FlowComputation.MAX_REPORTED_FLOWS).stream()
      .filter(f -> !f.isEmpty())
      .map(f -> addDereferenceMessage(f, syntaxNode))
      .collect(Collectors.toSet());
    reportIssue(syntaxNode, message, flows);
  }

  @Nullable
  private static Symbol dereferencedSymbol(Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree memberSelectExpr = ((MemberSelectExpressionTree) syntaxNode).expression();
      if (memberSelectExpr.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) memberSelectExpr).symbol();
      }
    }
    return null;
  }

  private static Flow addDereferenceMessage(Flow flow, Tree syntaxNode) {
    String symbolName = SyntaxTreeNameFinder.getName(syntaxNode);
    String msg;
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) syntaxNode).expression().is(Tree.Kind.METHOD_INVOCATION)) {
      msg = String.format("Result of '%s()' is dereferenced.", symbolName);
    } else {
      msg = String.format("'%s' is dereferenced.", symbolName);
    }
    return Flow.builder()
      .add(new JavaFileScannerContext.Location(msg, syntaxNode))
      .addAll(flow)
      .build();
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    NullDereferenceIssue issue = null;
    if (syntaxNode.is(Tree.Kind.SWITCH_STATEMENT)) {
      int numberOfCaseValues = ((SwitchStatementTree) syntaxNode)
        .cases()
        .stream()
        .flatMap(c -> c.labels().stream())
        .map(CaseLabelTree::expressions)
        .mapToInt(List::size)
        .sum();
      SymbolicValue conditionSymbolicValue = context.getState().peekValue(numberOfCaseValues);

      if (context.getConstraintManager().isNull(context.getState(), conditionSymbolicValue)) {
        issue = new NullDereferenceIssue(context.getNode(), conditionSymbolicValue, syntaxNode);
      }
    }

    if (syntaxNode.is(Tree.Kind.THROW_STATEMENT)) {
      SymbolicValue peek = context.getState().peekValue();
      if (peek != null && context.getConstraintManager().isNull(context.getState(), peek)) {
        issue = new NullDereferenceIssue(context.getNode(), peek, syntaxNode);
      }
    }

    if (issue != null) {
      detectedIssues.peek().add(issue);
      context.createSink();
      return context.getState();
    }

    List<ProgramState> programStates = setNullConstraint(context, syntaxNode);
    for (ProgramState programState : programStates) {
      context.addTransition(programState);
    }
    return context.getState();
  }

  private static List<ProgramState> setNullConstraint(CheckerContext context, Tree syntaxNode) {
    SymbolicValue val = context.getState().peekValue();
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION) &&
      ((MethodInvocationTree) syntaxNode).methodSymbol().metadata().nullabilityData().isStrongNullable(PACKAGE, false, false)) {
      Objects.requireNonNull(val);
      List<ProgramState> states = new ArrayList<>();
      states.addAll(val.setConstraint(context.getState(), ObjectConstraint.NULL));
      states.addAll(val.setConstraint(context.getState(), ObjectConstraint.NOT_NULL));
      return states;
    }
    return Collections.singletonList(context.getState());
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    EXCEPTIONAL_YIELD_CHECKER.reportOnExceptionalYield(context.getNode(), this);
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    reportIssues();
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    reportIssues();
  }

  private void reportIssues() {
    Set<NullDereferenceIssue> issues = detectedIssues.pop();
    issues.forEach(issue -> reportIssue(issue.symbolicValue, issue.tree, issue.node));
  }
}
