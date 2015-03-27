/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.collect.ImmutableMap;
import com.sonar.sslr.api.AstNode;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
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

import javax.annotation.Nullable;
import java.util.Map;

@Rule(
  key = "S1149",
  name = "Synchronized classes Vector, Hashtable, Stack and StringBuffer should not be used",
  tags = {"multi-threading", "performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("20min")
public class SynchronizedClassUsageCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String>builder()
    .put("java.util.Vector", "\"ArrayList\" or \"LinkedList\"")
    .put("java.util.Hashtable", "\"HashMap\"")
    .put("java.lang.StringBuffer", "\"StringBuilder\"")
    .put("java.util.Stack", "\"Deque\"")
    .build();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);

    boolean hasIssueOnDeclaredType = reportIssueIfDeprecatedType(tree.type());
    if (!hasIssueOnDeclaredType) {
      ExpressionTree init = tree.initializer();
      if (init != null && init.is(Tree.Kind.NEW_CLASS)) {
        reportIssueIfDeprecatedType(tree.initializer());
      }
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.returnType());
    scan(tree.defaultValue());
    scan(tree.block());
    if (!isOverriding(tree)) {
      for (VariableTree param : tree.parameters()) {
        reportIssueIfDeprecatedType(param.type());
      }
      reportIssueIfDeprecatedType(tree.returnType());
    }

  }

  @Override
  public void visitClass(ClassTree tree) {
    super.visitClass(tree);
    for (TypeTree parent : tree.superInterfaces()) {
      reportIssueIfDeprecatedType(parent);
    }

  }

  private boolean reportIssueIfDeprecatedType(@Nullable ExpressionTree tree) {
    if (tree == null) {
      return false;
    }
    return reportIssueIfDeprecatedType(tree.symbolType(), tree);
  }

  private boolean reportIssueIfDeprecatedType(@Nullable TypeTree tree) {
    if (tree == null) {
      return false;
    }
    return reportIssueIfDeprecatedType(tree.symbolType(), tree);
  }

  private boolean reportIssueIfDeprecatedType(org.sonar.plugins.java.api.semantic.Type symbolType, Tree tree) {
    for (String forbiddenTypeName : REPLACEMENTS.keySet()) {
      if (symbolType.is(forbiddenTypeName)) {
        reportIssue(tree, forbiddenTypeName);
        return true;
      }
    }
    return false;
  }

  private void reportIssue(Tree tree, String type) {
    String simpleTypeName = type.substring(type.lastIndexOf('.') + 1);
    context.addIssue(tree, this, "Replace the synchronized class \"" + simpleTypeName + "\" by an unsynchronized one such as " + REPLACEMENTS.get(type) + ".");
  }

  private boolean isOverriding(MethodTree tree) {
    for (AnnotationTree annotation : tree.modifiers().annotations()) {
      AstNode node = ((JavaTree) annotation).getAstNode();
      if (AstNodeTokensMatcher.matches(node, "@Override")) {
        return true;
      }
    }
    return false;
  }
}
