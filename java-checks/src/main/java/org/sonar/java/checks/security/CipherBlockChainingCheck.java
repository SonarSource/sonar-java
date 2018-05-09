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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3329")
public class CipherBlockChainingCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition("javax.crypto.spec.IvParameterSpec").name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Tree mTree = findParentMethod(newClassTree);
    if (mTree.is(Tree.Kind.METHOD)) {
      Optional<StatementTree> secureRandomOptional = findSecureRandomInMethod((MethodTree) mTree, returnSymbolOfArgument(newClassTree.arguments().get(0)));
      if (!secureRandomOptional.isPresent()) {
        reportIssue(newClassTree, "Use a dynamically-generated, random IV.");
      }
    }
  }

  private static Optional<StatementTree> findSecureRandomInMethod(MethodTree methodTree,
                                                                  @javax.annotation.Nullable Symbol argumentSymbol) {
    if (methodTree.block() != null && argumentSymbol != null) {
      return methodTree.block().body().stream().filter(statementTree -> {
        if (statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
          ExpressionTree expr = ((ExpressionStatementTree) statementTree).expression();
          if (expr.is(Tree.Kind.METHOD_INVOCATION)) {
            MethodInvocationTree mit = ((MethodInvocationTree) expr);
            if ("nextBytes".equals(mit.symbol().name())
              && argumentSymbol.equals(returnSymbolOfArgument(mit.arguments().get(0)))) {
              return true;
            }
          }
        } 
        return false;
      }).findFirst();
    }
    return Optional.empty();

  }

  @CheckForNull
  private static Symbol returnSymbolOfArgument(ExpressionTree argument) {
    Symbol symbolArgument = null;
    if (argument.is(Tree.Kind.IDENTIFIER)) {
      symbolArgument = ((IdentifierTree) argument).symbol();
    } else if (argument.is(Tree.Kind.MEMBER_SELECT)) {
      symbolArgument = ((MemberSelectExpressionTree) argument).identifier().symbol();
    }
    return symbolArgument;
  }

  private static Tree findParentMethod(Tree tree) {
    while (!((tree).is(Tree.Kind.METHOD)) && !tree.is(Tree.Kind.CLASS)) {
      tree = tree.parent();
    }
    return tree;
  }
}
