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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1210",
  name = "\"equals(Object obj)\" should be overridden along with the \"compareTo(T obj)\" method",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("15min")
public class EqualsNotOverridenWithCompareToCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isComparable(classTree)) {
      boolean hasEquals = false;
      Tree compare = null;

      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.METHOD)) {
          MethodTree method = (MethodTree) member;

          if (isEqualsMethod(method)) {
            hasEquals = true;
          } else if (isCompareToMethod(method)) {
            compare = member;
          }
        }
      }

      if (compare != null && !hasEquals) {
        addIssue(compare, "Override \"equals(Object obj)\" to comply with the contract of the \"compareTo(T o)\" method.");
      }
    }
  }

  private boolean isCompareToMethod(MethodTree method) {
    String name = method.simpleName().name();
    return "compareTo".equals(name) && returnsInt(method) && method.parameters().size() == 1;
  }

  private boolean isEqualsMethod(MethodTree method) {
    return ((MethodTreeImpl) method).isEqualsMethod();
  }

  private boolean isComparable(ClassTree tree) {
    for (Type type : tree.symbol().interfaces()) {
      if (type.is("java.lang.Comparable")) {
        return true;
      }
    }
    return false;
  }

  private boolean returnsInt(MethodTree tree) {
    TypeTree typeTree = tree.returnType();
    return typeTree != null && typeTree.symbolType().isPrimitive(Type.Primitives.INT);
  }

}
