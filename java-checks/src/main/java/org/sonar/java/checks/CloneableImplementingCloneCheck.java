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
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
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
    ClassTreeImpl classTree = (ClassTreeImpl) tree;
    TypeSymbol classSymbol = classTree.getSymbol();
    if (isCloneable(classTree) && !classSymbol.isAbstract() && !declaresCloneMethod(classSymbol)) {
      addIssue(tree, "Add a \"clone()\" method to this class.");
    }
  }

  private boolean declaresCloneMethod(TypeSymbol classSymbol) {
    for (Symbol memberSymbol : classSymbol.members().lookup("clone")) {
      if (memberSymbol.isKind(Symbol.MTH)) {
        MethodSymbol methodSymbol = (MethodSymbol) memberSymbol;
        if (methodSymbol.getParametersTypes().isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isCloneable(ClassTreeImpl classTree) {
    for (TypeTree superInterface : classTree.superInterfaces()) {
      if (superInterface.symbolType().is("java.lang.Cloneable")) {
        return true;
      }
    }
    return false;
  }

}
