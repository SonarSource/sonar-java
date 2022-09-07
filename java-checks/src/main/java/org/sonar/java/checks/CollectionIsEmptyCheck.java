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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1155")
public class CollectionIsEmptyCheck extends ExtendedIssueBuilderSubscriptionVisitor {

  private enum EmptyComparisonType {
    EMPTY, NOT_EMPTY
  }

  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
  private static final MethodMatchers SIZE_METHOD = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_COLLECTION)
    .names("size")
    .addWithoutParametersMatcher()
    .build();
  private static final Tree.Kind[] TARGETED_BINARY_OPERATOR_TREES = {
    Tree.Kind.EQUAL_TO,
    Tree.Kind.NOT_EQUAL_TO,
    Tree.Kind.LESS_THAN,
    Tree.Kind.LESS_THAN_OR_EQUAL_TO,
    Tree.Kind.GREATER_THAN,
    Tree.Kind.GREATER_THAN_OR_EQUAL_TO
  };
  private static final Tree.Kind[] CLASS_TREES = {
    Tree.Kind.CLASS,
    Tree.Kind.ENUM,
    Tree.Kind.INTERFACE,
    Tree.Kind.RECORD,
    Tree.Kind.ANNOTATION_TYPE
  };
  private static final Deque<Boolean> IS_COLLECTION_ENCLOSING_TYPES_STACK = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ListUtils.concat(Arrays.asList(CLASS_TREES), Arrays.asList(TARGETED_BINARY_OPERATOR_TREES));
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    // in case of polluted state
    IS_COLLECTION_ENCLOSING_TYPES_STACK.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(CLASS_TREES)) {
      handleClassTree((ClassTree) tree);
    } else {
      // Necessarily a BinaryExpressionTree - the rule only raises issues on size() comparisons
      // are "poorly" used from something which is NOT a Collection
      handleBinaryExpressionTree((BinaryExpressionTree) tree);
    }
  }

  private static void handleClassTree(ClassTree tree) {
    Symbol.TypeSymbol symbol = tree.symbol();
    boolean isCollection = symbol.type().isSubtypeOf(JAVA_UTIL_COLLECTION);
    if (isInnerClassOfCollection(symbol)) {
      // inner classes which might be related to its parent collection
      isCollection = true;
    }
    IS_COLLECTION_ENCLOSING_TYPES_STACK.push(isCollection);
  }

  private static boolean isInnerClassOfCollection(Symbol.TypeSymbol symbol) {
    return Boolean.TRUE.equals(IS_COLLECTION_ENCLOSING_TYPES_STACK.peek()) && !symbol.isStatic();
  }

  private void handleBinaryExpressionTree(BinaryExpressionTree tree) {
    if (isInCollectionType()) {
      return;
    }
    getCallToSizeInvocation(tree).ifPresent(callToSizeInvocation ->
      getEmptyComparisonType(tree).ifPresent(comparisonType -> newIssue()
        .onTree(tree)
        .withMessage("Use isEmpty() to check whether the collection is empty or not.")
        .withQuickFix(() -> getQuickFix(tree, callToSizeInvocation, comparisonType))
        .report()));
  }

  private static boolean isInCollectionType() {
    return Boolean.TRUE.equals(IS_COLLECTION_ENCLOSING_TYPES_STACK.peek());
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(CLASS_TREES)) {
      IS_COLLECTION_ENCLOSING_TYPES_STACK.pop();
    }
  }

  private static JavaQuickFix getQuickFix(BinaryExpressionTree tree, MethodInvocationTree callToSizeInvocation, EmptyComparisonType emptyComparisonType) {
    IdentifierTree sizeCallIdentifier = ExpressionUtils.methodName(callToSizeInvocation);
    // We want to keep the object on which "size" is called, we therefore replace everything before with ! (if needed) and after with "isEmpty()".
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix("Use \"isEmpty()\"");

    AnalyzerMessage.TextSpan textSpan = textSpanBetween(tree.firstToken(), true, callToSizeInvocation, false);
    String replacement = emptyComparisonType == EmptyComparisonType.EMPTY ? "" : "!";
    if (!(textSpan.isEmpty() && replacement.isEmpty())) {
      builder.addTextEdit(JavaTextEdit.replaceTextSpan(textSpan, replacement));
    }

    builder.addTextEdit(JavaTextEdit.replaceTextSpan(textSpanBetween(sizeCallIdentifier, true, tree.lastToken(), true), "isEmpty()"));
    return builder.build();
  }

  private static Optional<MethodInvocationTree> getCallToSizeInvocation(BinaryExpressionTree tree) {
    return getCallToSizeInvocation(tree.leftOperand())
      // we stop at the first match
      .or(() -> getCallToSizeInvocation(tree.rightOperand()));
  }

  private static Optional<MethodInvocationTree> getCallToSizeInvocation(ExpressionTree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocationTree = (MethodInvocationTree) tree;
      if (SIZE_METHOD.matches(invocationTree)) {
        return Optional.of(invocationTree);
      }
    }
    return Optional.empty();
  }

  private static Optional<EmptyComparisonType> getEmptyComparisonType(BinaryExpressionTree tree) {
    boolean leftIsZero = LiteralUtils.isZero(tree.leftOperand());
    boolean leftIsOne = LiteralUtils.isOne(tree.leftOperand());
    boolean rightIsZero = LiteralUtils.isZero(tree.rightOperand());
    boolean rightIsOne = LiteralUtils.isOne(tree.rightOperand());
    boolean anyZero = leftIsZero || rightIsZero;

    if (isEmptyComparison(tree, leftIsZero, leftIsOne, rightIsZero, rightIsOne, anyZero)) {
      return Optional.of(EmptyComparisonType.EMPTY);
    }
    if (isNotEmptyComparison(tree, leftIsZero, leftIsOne, rightIsZero, rightIsOne, anyZero)) {
      return Optional.of(EmptyComparisonType.NOT_EMPTY);
    }
    return Optional.empty();
  }

  private static boolean isEmptyComparison(BinaryExpressionTree tree, boolean leftIsZero, boolean leftIsOne, boolean rightIsZero, boolean rightIsOne, boolean anyZero) {
    // size == 0, 0 == size
    return (tree.is(Tree.Kind.EQUAL_TO) && anyZero)
      // size > 1
      || (tree.is(Tree.Kind.LESS_THAN) && rightIsOne)
      // size <= 0
      || (tree.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO) && rightIsZero)
      // 1 > size
      || (tree.is(Tree.Kind.GREATER_THAN) && leftIsOne)
      // 0 >= size
      || (tree.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO) && leftIsZero);
  }

  private static boolean isNotEmptyComparison(BinaryExpressionTree tree, boolean leftIsZero, boolean leftIsOne, boolean rightIsZero, boolean rightIsOne, boolean anyZero) {
    // size != 0, 0 != size
    return (tree.is(Tree.Kind.NOT_EQUAL_TO) && anyZero)
      // size > 0
      || (tree.is(Tree.Kind.GREATER_THAN) && rightIsZero)
      // size >= 1
      || (tree.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO) && rightIsOne)
      // 0 < size
      || (tree.is(Tree.Kind.LESS_THAN) && leftIsZero)
      // 1 <= size
      || (tree.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO) && leftIsOne);
  }

}
