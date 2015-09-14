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
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1206",
  name = "\"equals(Object obj)\" and \"hashCode()\" should be overridden in pairs",
  tags = {"bug", "cert", "cwe"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("15min")
public class EqualsOverridenWithHashCodeCheck extends SubscriptionBaseVisitor {

  private static final String HASHCODE = "hashCode";
  private static final String EQUALS = "equals";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }
  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.is(Tree.Kind.CLASS)) {
      MethodTree equalsMethod = null;
      MethodTree hashCodeMethod = null;
      for (Tree memberTree : classTree.members()) {
        if (memberTree.is(Tree.Kind.METHOD)) {
          MethodTree methodTree = (MethodTree) memberTree;
          if (isEquals(methodTree)) {
            equalsMethod = methodTree;
          } else if (isHashCode(methodTree)) {
            hashCodeMethod = methodTree;
          }
        }
      }

      if (equalsMethod != null && hashCodeMethod == null) {
        addIssue(equalsMethod, getMessage(EQUALS, HASHCODE));
      } else if (hashCodeMethod != null && equalsMethod == null) {
        addIssue(hashCodeMethod, getMessage(HASHCODE, EQUALS));
      }
    }
  }

  private static boolean isEquals(MethodTree methodTree) {
    return ((MethodTreeImpl) methodTree).isEqualsMethod();
  }

  private static boolean isHashCode(MethodTree methodTree) {
    return HASHCODE.equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty() && returnsInt(methodTree);
  }

  private static boolean returnsInt(MethodTree tree) {
    TypeTree typeTree = tree.returnType();
    return typeTree != null && typeTree.symbolType().isPrimitive(org.sonar.plugins.java.api.semantic.Type.Primitives.INT);
  }

  private static String getMessage(String overridenMethod, String methodToOverride) {
    return "This class overrides \"" + overridenMethod + "()\" and should therefore also override \"" + methodToOverride + "()\".";
  }

}
