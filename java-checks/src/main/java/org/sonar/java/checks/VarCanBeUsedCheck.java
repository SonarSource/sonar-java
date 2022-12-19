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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6212")
public class VarCanBeUsedCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  public static final String MESSAGE = "Declare this local variable with \"var\" instead.";
  private int typeAssignmentLine = -1;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.VARIABLE);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava10Compatible();
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    typeAssignmentLine = -1;
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    ExpressionTree initializer = variableTree.initializer();
    TypeTree type = variableTree.type();
    Type symbolType = type.symbolType();
    String typeName = symbolType.name().toLowerCase(Locale.ROOT);
    IdentifierTree identifierTree = variableTree.simpleName();

    if (isMultiAssignment(variableTree) ||
      initializer == null ||
      type.is(Tree.Kind.VAR_TYPE) ||
      isArrayInitializerWithoutType(initializer) ||
      symbolType.isUnknown() ||
      !JUtils.isLocalVariable(variableTree.symbol()) ||
      symbolType.isParameterized()) {
      return;
    }

    initializer = ExpressionUtils.skipParentheses(initializer);

    if (isExcludedInitializer(initializer)) {
      return;
    }

    Type initializerType = initializer.symbolType();
    if (symbolType.fullyQualifiedName().equals(initializerType.fullyQualifiedName()) &&
      (isSelfExplained(initializer) ||
        typeWasMentionedInTheName(identifierTree, typeName) ||
        typeWasMentionedInTheInitializer(initializer, typeName))) {
      reportIssue(identifierTree, MESSAGE);
    }
  }

  private static boolean isExcludedInitializer(ExpressionTree initializer) {
    if (initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol.MethodSymbol symbol = ((MethodInvocationTree) initializer).symbol();
      return !symbol.isUnknown() && JUtils.isParametrizedMethod(symbol);
    }
    return initializer.is(Tree.Kind.CONDITIONAL_EXPRESSION, Tree.Kind.METHOD_REFERENCE, Tree.Kind.LAMBDA_EXPRESSION);
  }

  private static boolean typeWasMentionedInTheName(IdentifierTree variable, String type) {
    return isLogicallyReferable(variable.name(), type);
  }

  private static boolean isSelfExplained(ExpressionTree initializer) {
    return initializer.is(Tree.Kind.NEW_CLASS, Tree.Kind.NEW_ARRAY) || initializer instanceof LiteralTree;
  }

  private static boolean typeWasMentionedInTheInitializer(ExpressionTree initializer, String type) {
    if (initializer.is(Tree.Kind.IDENTIFIER) && typeWasMentionedInTheName(((IdentifierTree) initializer), type)) {
      return true;
    } else if (initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      ExpressionTree methodSelect = ExpressionUtils.skipParentheses(((MethodInvocationTree) initializer).methodSelect());
      if (methodSelect.is(Tree.Kind.IDENTIFIER) && typeWasMentionedInTheName((IdentifierTree) methodSelect, type)) {
        return true;
      } else if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodSelect;
        return typeWasMentionedInTheName(memberSelectExpressionTree.identifier(), type) ||
          (memberSelectExpressionTree.expression().is(Tree.Kind.IDENTIFIER) &&
            typeWasMentionedInTheName((IdentifierTree) memberSelectExpressionTree.expression(), type));
      }
    }
    return false;
  }

  private static boolean isLogicallyReferable(String identifierName, String typeName) {
    return identifierName.toLowerCase(Locale.ROOT).contains(typeName);
  }

  private boolean isMultiAssignment(VariableTree variableTree) {
    SyntaxToken firstToken = variableTree.type().firstToken();
    if (firstToken == null) {
      return false;
    }
    int line = firstToken.range().start().line();
    if (typeAssignmentLine == line) {
      return true;
    }
    typeAssignmentLine = line;
    SyntaxToken token = variableTree.endToken();
    return token != null && ",".equals(token.text());
  }

  private static boolean isArrayInitializerWithoutType(ExpressionTree initializer) {
    return initializer.is(Tree.Kind.NEW_ARRAY) && ((NewArrayTree) initializer).newKeyword() == null;
  }
}
