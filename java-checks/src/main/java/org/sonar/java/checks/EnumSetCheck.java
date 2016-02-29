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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1641",
  name = "Sets with elements that are enum values should be replaced with EnumSet",
  priority = Priority.MAJOR,
  tags = {Tag.PERFORMANCE})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class EnumSetCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variableTree = (VariableTree) tree;
    Type variableType = variableTree.symbol().type();
    ExpressionTree initializer = variableTree.initializer();
    boolean reportIssueOnInitializer = false;
    if (initializer != null) {
      reportIssueOnInitializer = reportIssueOnType(initializer.symbolType(), initializer) || initializer.symbolType().isSubtypeOf("java.util.EnumSet");
    }
    if (!reportIssueOnInitializer) {
      reportIssueOnType(variableType, variableTree.type());
    }

  }

  private boolean reportIssueOnType(Type type, Tree reportTree) {
    if (type.isSubtypeOf("java.util.Set") && !type.isSubtypeOf("java.util.EnumSet") && type instanceof JavaType.ParametrizedTypeJavaType) {
      JavaType.ParametrizedTypeJavaType parametrizedType = (JavaType.ParametrizedTypeJavaType) type;
      List<JavaType.TypeVariableJavaType> typeParameters = parametrizedType.typeParameters();
      if(!typeParameters.isEmpty()) {
        Type typeParameter = parametrizedType.substitution(typeParameters.get(0));
        if (typeParameter != null && typeParameter.symbol().isEnum()) {
          reportIssue(reportTree, "Convert this Set to an EnumSet.");
          return true;
        }
      }
    }
    return false;
  }

}
