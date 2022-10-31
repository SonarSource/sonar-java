/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S864")
public class OperatorPrecedenceCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<OperatorRelation, Boolean> OPERATORS_RELATION_TABLE = new HashMap<>();

  private static final Set<Tree.Kind> ARITHMETIC_OPERATORS = EnumSet.of(
    Tree.Kind.MINUS,
    Tree.Kind.REMAINDER,
    Tree.Kind.MULTIPLY,
    Tree.Kind.PLUS
    );

  private static final Set<Tree.Kind> EQUALITY_RELATIONAL_OPERATORS = EnumSet.of(
    Tree.Kind.EQUAL_TO,
    Tree.Kind.GREATER_THAN,
    Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
    Tree.Kind.LESS_THAN,
    Tree.Kind.LESS_THAN_OR_EQUAL_TO,
    Tree.Kind.NOT_EQUAL_TO
    );

  private static final Set<Tree.Kind> SHIFT_OPERATORS = EnumSet.of(
    Tree.Kind.LEFT_SHIFT,
    Tree.Kind.RIGHT_SHIFT,
    Tree.Kind.UNSIGNED_RIGHT_SHIFT
    );

  private static final Tree.Kind[] CONDITIONAL_EXCLUSIONS = new Tree.Kind[]{
      Tree.Kind.METHOD_INVOCATION, Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT,
      Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.TYPE_CAST, Tree.Kind.NEW_CLASS,
      Tree.Kind.ARRAY_ACCESS_EXPRESSION, Tree.Kind.NEW_ARRAY, Tree.Kind.METHOD_REFERENCE
  };

  static {
    put(ARITHMETIC_OPERATORS, SetUtils.concat(SHIFT_OPERATORS, EnumSet.of(Tree.Kind.AND, Tree.Kind.XOR, Tree.Kind.OR)));
    put(SHIFT_OPERATORS, SetUtils.concat(ARITHMETIC_OPERATORS, EnumSet.of(Tree.Kind.AND, Tree.Kind.XOR, Tree.Kind.OR)));
    put(EnumSet.of(Tree.Kind.AND), SetUtils.concat(ARITHMETIC_OPERATORS, SHIFT_OPERATORS, EnumSet.of(Tree.Kind.XOR, Tree.Kind.OR)));
    put(EnumSet.of(Tree.Kind.XOR), SetUtils.concat(ARITHMETIC_OPERATORS, SHIFT_OPERATORS, EnumSet.of(Tree.Kind.AND, Tree.Kind.OR)));
    put(EnumSet.of(Tree.Kind.OR), SetUtils.concat(ARITHMETIC_OPERATORS, SHIFT_OPERATORS, EnumSet.of(Tree.Kind.AND, Tree.Kind.XOR)));
    put(EnumSet.of(Tree.Kind.CONDITIONAL_AND), EnumSet.of(Tree.Kind.CONDITIONAL_OR));
    put(EnumSet.of(Tree.Kind.CONDITIONAL_OR), EnumSet.of(Tree.Kind.CONDITIONAL_AND));
  }

  private JavaFileScannerContext context;
  private Deque<Tree.Kind> stack = new LinkedList<>();
  private Set<Integer> reportedLines = new HashSet<>();

  private static void put(Iterable<Tree.Kind> firstSet, Iterable<Tree.Kind> secondSet) {
    for (Tree.Kind first : firstSet) {
      for (Tree.Kind second : secondSet) {
        OPERATORS_RELATION_TABLE.put(new OperatorRelation(first, second), true);
      }
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    reportedLines.clear();
    scan(context.getTree());
    reportedLines.clear();
  }

  @Override
  public void visitAnnotation(AnnotationTree tree) {
    stack.push(null);
    for (ExpressionTree argument : tree.arguments()) {
      if (argument.is(Tree.Kind.ASSIGNMENT)) {
        scan(((AssignmentExpressionTree) argument).expression());
      } else {
        scan(argument);
      }
    }
    stack.pop();
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    scan(tree.expression());
    stack.push(null);
    scan(tree.dimension());
    stack.pop();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    Tree.Kind peek = stack.peek();
    Tree.Kind kind = tree.kind();
    if (requiresParenthesis(peek, kind)) {
      raiseIssue(tree.operatorToken().range().start().line(), tree);
    }
    stack.push(kind);
    super.visitBinaryExpression(tree);
    stack.pop();
  }

  private static boolean requiresParenthesis(Tree.Kind kind1, Tree.Kind kind2) {
    return BooleanUtils.isTrue(OPERATORS_RELATION_TABLE.get(new OperatorRelation(kind1, kind2)));
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    super.visitIfStatement(tree);
    ExpressionTree condition = tree.condition();
    if (condition.is(Tree.Kind.ASSIGNMENT) && EQUALITY_RELATIONAL_OPERATORS.contains(((AssignmentExpressionTree) condition).expression().kind())) {
      raiseIssue(((AssignmentExpressionTree) condition).operatorToken().range().start().line(), tree);
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    scan(tree.methodSelect());
    scan(tree.typeArguments());
    for (ExpressionTree argument : tree.arguments()) {
      stack.push(null);
      scan(argument);
      stack.pop();
    }
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    stack.push(null);
    super.visitNewArray(tree);
    stack.pop();
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    stack.push(null);
    super.visitNewClass(tree);
    stack.pop();
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    stack.push(null);
    super.visitParenthesized(tree);
    stack.pop();
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    checkConditionalOperand(tree.trueExpression());
    checkConditionalOperand(tree.falseExpression());
    super.visitConditionalExpression(tree);
  }

  private void checkConditionalOperand(ExpressionTree tree) {
    if (tree.is(CONDITIONAL_EXCLUSIONS)
      || tree instanceof LiteralTree
      || tree instanceof UnaryExpressionTree
      || isSimpleLambda(tree)) {
      return;
    }
    raiseIssue(tree.firstToken().range().start().line(), tree);
  }

  private static boolean isSimpleLambda(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      return false;
    }
    Tree body = ((LambdaExpressionTree) tree).body();
    return body instanceof LiteralTree
      || body instanceof UnaryExpressionTree
      || body.is(Tree.Kind.IDENTIFIER);
  }

  private void raiseIssue(int line, Tree tree) {
    if (reportedLines.add(line)) {
      context.reportIssue(this, tree, "Add parentheses to make the operator precedence explicit.");
    }
  }
  
  private static final class OperatorRelation {
    private final Tree.Kind first;
    private final Tree.Kind second;

    public OperatorRelation(Tree.Kind first, Tree.Kind second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OperatorRelation that = (OperatorRelation) o;
      return first == that.first &&
        second == that.second;
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }
  } 

}
