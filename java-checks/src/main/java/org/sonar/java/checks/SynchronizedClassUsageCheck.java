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
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Map;

@Rule(
  key = "S1149",
  name = "Synchronized classes Vector, Hashtable, Stack and StringBuffer should not be used",
  tags = {"multi-threading", "performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("20min")
public class SynchronizedClassUsageCheck extends SubscriptionBaseVisitor {

  private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String>builder()
    .put("java.util.Vector", "\"ArrayList\" or \"LinkedList\"")
    .put("java.util.Hashtable", "\"HashMap\"")
    .put("java.lang.StringBuffer", "\"StringBuilder\"")
    .put("java.util.Stack", "\"Deque\"")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    tree.accept(new DeprecatedTypeVisitor());
  }

  private static boolean isDeprecatedType(Type symbolType) {
    if (symbolType.isClass()) {
      for (String deprecatedType : REPLACEMENTS.keySet()) {
        if (symbolType.is(deprecatedType)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isOverriding(MethodTree tree) {
    return BooleanUtils.isTrue(((MethodTreeImpl) tree).isOverriding());
  }

  private class DeprecatedTypeVisitor extends BaseTreeVisitor {

    @Override
    public void visitClass(ClassTree tree) {
      TypeTree superClass = tree.superClass();
      if (superClass != null) {
        reportIssueOnDeprecatedType(tree, superClass.symbolType());
      }

      scan(tree.members());
    }

    @Override
    public void visitMethod(MethodTree tree) {
      TypeTree returnTypeTree = tree.returnType();
      if (!isOverriding(tree) || returnTypeTree == null) {
        if (returnTypeTree != null) {
          reportIssueOnDeprecatedType(returnTypeTree, returnTypeTree.symbolType());
        }
        scan(tree.parameters());
      }
      scan(tree.block());
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ExpressionTree initializer = tree.initializer();
      if (!reportIssueOnDeprecatedType(tree.type(), tree.symbol().type()) && initializer != null && !initializer.is(Tree.Kind.METHOD_INVOCATION)) {
        reportIssueOnDeprecatedType(initializer, initializer.symbolType());
      }
    }

    private boolean reportIssueOnDeprecatedType(Tree tree, Type type) {
      if (isDeprecatedType(type)) {
        addIssue(tree, "Replace the synchronized class \"" + type.name() + "\" by an unsynchronized one such as " + REPLACEMENTS.get(type.fullyQualifiedName()) + ".");
        return true;
      }
      return false;
    }
  }
}
