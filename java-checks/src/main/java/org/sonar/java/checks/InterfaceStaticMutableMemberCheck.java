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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "S2386",
  name = "Interfaces should not have \"public static\" mutable fields",
  tags = {"unpredictable"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("30min")
public class InterfaceStaticMutableMemberCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    for (Tree member : ((ClassTree) tree).members()) {
      if (member.is(Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        if (isStaticMember(variableTree) && isMutableMember(variableTree)) {
          addIssue(variableTree, MessageFormat.format("Move \"{0}\" to a class and lower its visibility", variableTree.simpleName().name()));
        }
      }
    }
  }

  private static boolean isStaticMember(VariableTree variableTree) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC);
  }

  private static boolean isMutableMember(VariableTree variableTree) {
    return isArray(variableTree.type()) || isDateOrCollection(variableTree.type().symbolType());
  }

  private static boolean isArray(Tree typeTree) {
    return typeTree.is(Kind.ARRAY_TYPE);
  }

  private static boolean isDateOrCollection(Type variableSymbolType) {
    return variableSymbolType.is("java.util.Date") ||
        (variableSymbolType.isSubtypeOf("java.util.Collection") && !variableSymbolType.isSubtypeOf("com.google.common.collect.ImmutableCollection"));
  }
}
