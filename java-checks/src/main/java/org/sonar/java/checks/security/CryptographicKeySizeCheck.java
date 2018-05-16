/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4426")
public class CryptographicKeySizeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodVisitor methodVisitor = new MethodVisitor();
    tree.accept(methodVisitor);
  }

  private class MethodVisitor extends BaseTreeVisitor {

    private String algorithm = null;
    private Symbol geInstanceKeyGenSymbol = null;

    @Override
    public void visitNewClass(NewClassTree tree) {
      // skip anonymous classes
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isKeyGenGetInstanceOrInitMethod(tree)) {
        String methodName = tree.symbol().name();
        ExpressionTree arg = tree.arguments().get(0);
        if ("getInstance".equals(methodName)) {
          if (arg.is(Tree.Kind.STRING_LITERAL)) {
            extractSymbolOfAssignedInstance(tree).ifPresent(keyGenSymbol -> {
              algorithm = LiteralUtils.trimQuotes(((LiteralTree) arg).value());
              geInstanceKeyGenSymbol = keyGenSymbol;
            });
          }
        } else if (isInitMethod(methodName) && initializeIsCalledOnPreviouslyDefinedKeyGen(tree)) {
          Integer minKeySize = findMinimumKeySizeFromAlgorithm(algorithm);
          if (minKeySize != -1) {
            checkAlgorithmParameterToReport(arg, minKeySize)
              .ifPresent(reportString -> reportIssue(tree, reportString));
          }
        }
      }
    }

    private boolean isKeyGenGetInstanceOrInitMethod(MethodInvocationTree mit) {
      Symbol mitSymbol = mit.symbol();
      if (mitSymbol.type() != null) {
        String keyGenName = mitSymbol.type().fullyQualifiedName();
        return ("javax.crypto.KeyGenerator".equals(keyGenName) || "java.security.KeyPairGenerator".equals(keyGenName))
          && !mit.arguments().isEmpty();
      }
      return false;
    }

    private boolean isInitMethod(String methodName) {
      return ("init".equals(methodName)) || ("initialize".equals(methodName));
    }

    private Optional<String> checkAlgorithmParameterToReport(ExpressionTree argument, Integer keySize) {
      String reportString = null;
      if (argument.is(Tree.Kind.INT_LITERAL) && (Integer.parseInt(((LiteralTree) argument).value()) < keySize)) {
        reportString = "Use a key length of at least " + keySize + " bits.";
      }
      return Optional.ofNullable(reportString);
    }

    private int findMinimumKeySizeFromAlgorithm(String algorithm) {
      switch (algorithm) {
        case "Blowfish":
          return 128;
        case "RSA":
          return 2048;
        default:
          return -1;
      }
    }

    private Optional<Symbol> extractSymbolOfAssignedInstance(MethodInvocationTree mit) {
      Tree parentExpression = mit.parent();
      Symbol keyGenSymbol = null;
      if (parentExpression.is(Tree.Kind.VARIABLE)) {
        keyGenSymbol = ((VariableTree) parentExpression).symbol();
      } else if (parentExpression.is(Tree.Kind.ASSIGNMENT)) {
        ExpressionTree variable = ((AssignmentExpressionTree) parentExpression).variable();
        if (variable.is(Tree.Kind.IDENTIFIER)) {
          keyGenSymbol = ((IdentifierTree) variable).symbol();
        } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
          keyGenSymbol = ((MemberSelectExpressionTree) variable).identifier().symbol();
        }
      }
      return Optional.ofNullable(keyGenSymbol);
    }

    private boolean initializeIsCalledOnPreviouslyDefinedKeyGen(MethodInvocationTree mit) {
      boolean isValidKeyGenSymbol = false;
      if (geInstanceKeyGenSymbol != null) {
        ExpressionTree expr = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
        if (expr.is(Tree.Kind.IDENTIFIER)) {
          isValidKeyGenSymbol = geInstanceKeyGenSymbol.equals(((IdentifierTree) expr).symbol());
        } else if (expr.is(Tree.Kind.MEMBER_SELECT)) {
          isValidKeyGenSymbol = geInstanceKeyGenSymbol.equals(((MemberSelectExpressionTree) expr).identifier().symbol());
        }
      }
      return isValidKeyGenSymbol;
    }
  }
}
