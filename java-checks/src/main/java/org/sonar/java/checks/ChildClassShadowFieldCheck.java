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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2387",
  name = "Child class members should not shadow parent class members",
  tags = {"confusing"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class ChildClassShadowFieldCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = ((ClassTree) tree);
    if (classTree.superClass() != null) {
      Symbol.TypeSymbol superclassSymbol = classTree.superClass().symbolType().symbol();
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          String fieldName = ((VariableTree) member).simpleName().name();
          Symbol.TypeSymbol definingClass = getDefiningClassInHierarchyForField(superclassSymbol, fieldName);
          if (definingClass != null) {
            addIssue(member, String.format("\"%s\" is the name of a field in \"%s\".", fieldName, definingClass.name()));
          }
        }
      }
    }
  }

  @CheckForNull
  private Symbol.TypeSymbol getDefiningClassInHierarchyForField(Symbol.TypeSymbol classSymbol, String fieldName) {
    for (Symbol.TypeSymbol symbol = classSymbol; symbol != null; symbol = getSuperclass(symbol)) {
      for (Symbol member : symbol.memberSymbols()) {
        if (member.isVariableSymbol() && member.name().equals(fieldName)) {
          return symbol;
        }
      }
    }
    return null;
  }

  @CheckForNull
  private Symbol.TypeSymbol getSuperclass(Symbol.TypeSymbol symbol) {
    Type superType = symbol.superClass();
    if (superType != null) {
      return superType.symbol();
    }
    return null;
  }

}
