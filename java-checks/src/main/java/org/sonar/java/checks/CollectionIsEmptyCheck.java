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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1155")
public class CollectionIsEmptyCheck extends BaseTreeVisitor implements JavaFileScanner {

  private enum EmptyComparisonType {
    EMPTY, NOT_EMPTY
  }

  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
  private static final MethodMatchers SIZE_METHOD = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_COLLECTION)
    .names("size")
    .addWithoutParametersMatcher()
    .build();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (!tree.symbol().type().isSubtypeOf(JAVA_UTIL_COLLECTION) || !member.is(Tree.Kind.METHOD)) {
        scan(member);
      }
    }
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);

    getCallToSizeInvocation(tree).ifPresent(callToSizeInvocation ->
      getEmptyComparisonType(tree).ifPresent(comparisonType ->
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(tree)
          .withMessage("Use isEmpty() to check whether the collection is empty or not.")
          .withQuickFix(() -> getQuickFix(tree, callToSizeInvocation, comparisonType))
          .report()
      ));
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

    builder.addTextEdit(JavaTextEdit.replaceTextSpan(textSpanBetween(sizeCallIdentifier, true, tree.lastToken(), true),
      "isEmpty()"));
    return builder.build();
  }

  private static Optional<MethodInvocationTree> getCallToSizeInvocation(BinaryExpressionTree tree) {
    Optional<MethodInvocationTree> leftCallToSize = getCallToSizeInvocation(tree.leftOperand());
    if (leftCallToSize.isPresent()) {
      return leftCallToSize;
    }
    return getCallToSizeInvocation(tree.rightOperand());
  }

  private static Optional<MethodInvocationTree> getCallToSizeInvocation(ExpressionTree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
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
    return (tree.is(Kind.EQUAL_TO) && anyZero)
      // size > 1
      || (tree.is(Kind.LESS_THAN) && rightIsOne)
      // size <= 0
      || (tree.is(Kind.LESS_THAN_OR_EQUAL_TO) && rightIsZero)
      // 1 > size
      || (tree.is(Kind.GREATER_THAN) && leftIsOne)
      // 0 >= size
      || (tree.is(Kind.GREATER_THAN_OR_EQUAL_TO) && leftIsZero);
  }

  private static boolean isNotEmptyComparison(BinaryExpressionTree tree, boolean leftIsZero, boolean leftIsOne, boolean rightIsZero, boolean rightIsOne, boolean anyZero) {
    // size != 0, 0 != size
    return (tree.is(Kind.NOT_EQUAL_TO) && anyZero)
      // size > 0
      || (tree.is(Kind.GREATER_THAN) && rightIsZero)
      // size >= 1
      || (tree.is(Kind.GREATER_THAN_OR_EQUAL_TO) && rightIsOne)
      // 0 < size
      || (tree.is(Kind.LESS_THAN) && leftIsZero)
      // 1 <= size
      || (tree.is(Kind.LESS_THAN_OR_EQUAL_TO) && leftIsOne);
  }

}
