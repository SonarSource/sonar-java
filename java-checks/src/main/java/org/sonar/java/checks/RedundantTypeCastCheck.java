/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.WildcardTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S1905",
  name = "Redundant casts should not be used",
  priority = Priority.MINOR,
  tags = {Tag.CLUMSY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class RedundantTypeCastCheck extends IssuableSubscriptionVisitor {

  private Set<Tree> excluded = Sets.newHashSet();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    excluded.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TYPE_CAST, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS)) {
      addArgsToExclusion(tree);
    } else {
      TypeCastTree typeCastTree = (TypeCastTree) tree;
      JavaType cast = (JavaType) typeCastTree.type().symbolType();
      ExpressionTree expression = typeCastTree.expression();
      if (isChainedCastWithWildcard(typeCastTree)) {
        excluded.add(expression);
      }
      if (!excluded.contains(tree)) {
        JavaType expressionType = (JavaType) expression.symbolType();
        if (!isExcluded(cast) && (isRedundantNumericalCast(cast, expressionType) || isRedundantCast(cast, expressionType))) {
          reportIssue(typeCastTree.type(), "Remove this unnecessary cast to \"" + cast + "\".");
        }
      }
    }
  }

  private static boolean isChainedCastWithWildcard(TypeCastTree typeCastTree) {
    ExpressionTree expression = typeCastTree.expression();
    return expression.is(Tree.Kind.TYPE_CAST) && usesWildCard(expression);
  }

  private void addArgsToExclusion(Tree tree) {
    List<ExpressionTree> args;
    if(tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      args = mit.arguments();
    } else {
      NewClassTree newClassTree = (NewClassTree) tree;
      args = newClassTree.arguments();
    }
    for (ExpressionTree arg : args) {
      if (arg.is(Tree.Kind.TYPE_CAST)) {
        excluded.add(arg);
      }
    }
  }

  private static boolean isExcluded(JavaType cast) {
    return cast.isUnknown();
  }

  private static boolean isRedundantCast(JavaType cast, JavaType expressionType) {
    JavaType erasedExpressionType = expressionType;
    if(erasedExpressionType.isTagged(JavaType.TYPEVAR)) {
      erasedExpressionType = erasedExpressionType.erasure();
    }
    boolean expressionIsParametrized = isParametrizedType(expressionType);
    boolean castIsParametrized = isParametrizedType(cast);
    if (castIsParametrized ^ expressionIsParametrized) {
      return expressionIsParametrized && expressionType.erasure() != cast.erasure() && expressionType.isSubtypeOf(cast);
    }
    if (castIsParametrized) {
      return expressionType.isSubtypeOf(cast);
    }
    return erasedExpressionType.equals(cast) || (!cast.isNumerical() && erasedExpressionType.isSubtypeOf(cast));
  }

  private static boolean isParametrizedType(JavaType cast) {
    return cast instanceof JavaType.ParametrizedTypeJavaType;
  }

  private static boolean isRedundantNumericalCast(JavaType cast, JavaType expressionType) {
    return cast.isNumerical() && cast.equals(expressionType);
  }

  private static boolean usesWildCard(ExpressionTree expression) {
    WildCardFinder visitor = new WildCardFinder();
    expression.accept(visitor);
    return visitor.foundWildcard;
  }

  private static class WildCardFinder extends BaseTreeVisitor {
    private boolean foundWildcard = false;

    @Override
    public void visitWildcard(WildcardTree tree) {
      foundWildcard = true;
    }

  }
}
