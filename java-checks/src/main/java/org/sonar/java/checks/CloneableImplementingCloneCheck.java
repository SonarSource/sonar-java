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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2157",
  name = "\"Cloneables\" should implement \"clone\"",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class CloneableImplementingCloneCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isCloneable(classTree) && !classSymbol.isAbstract() && !declaresCloneMethod(classSymbol)) {
      addIssue(tree, "Add a \"clone()\" method to this class.");
    }
  }

  private static boolean declaresCloneMethod(Symbol.TypeSymbol classSymbol) {
    for (Symbol memberSymbol : classSymbol.lookupSymbols("clone")) {
      if (memberSymbol.isMethodSymbol()) {
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) memberSymbol;
        if (methodSymbol.parameterTypes().isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isCloneable(ClassTree classTree) {
    for (TypeTree superInterface : classTree.superInterfaces()) {
      if (superInterface.symbolType().is("java.lang.Cloneable")) {
        return true;
      }
    }
    return false;
  }

}
