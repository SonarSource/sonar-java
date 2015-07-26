/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S1905",
  name = "Redundant casts should not be used",
  tags = {"clumsy"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class RedundantTypeCastCheck extends SubscriptionBaseVisitor {

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
    } else if (!excluded.contains(tree)) {
      TypeCastTree typeCastTree = (TypeCastTree) tree;
      JavaType cast = (JavaType) typeCastTree.type().symbolType();
      JavaType expressionType = (JavaType) typeCastTree.expression().symbolType();
      if (!isExcluded(cast) && (isRedundantNumericalCast(cast, expressionType) || isRedundantCast(cast, expressionType))) {
        addIssue(tree, "Remove this unnecessary cast to \"" + cast + "\".");
      }
    }
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
    return erasedExpressionType.equals(cast) || (!(cast instanceof JavaType.ParametrizedTypeJavaType) && !cast.isNumerical() && erasedExpressionType.isSubtypeOf(cast));
  }

  private static boolean isRedundantNumericalCast(JavaType cast, JavaType expressionType) {
    return cast.isNumerical() && cast.equals(expressionType);
  }
}
