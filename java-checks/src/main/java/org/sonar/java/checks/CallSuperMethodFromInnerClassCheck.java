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
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Types;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2388",
  name = "Inner class calls to super class methods should be unambiguous",
  tags = {"pitfall"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class CallSuperMethodFromInnerClassCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTreeImpl classTree = (ClassTreeImpl) tree;
    Symbol.TypeSymbol classSymbol = classTree.getSymbol();
    if (classSymbol != null && isInnerClass(classSymbol)) {
      classTree.accept(new MethodInvocationVisitor(classSymbol));
    }
  }

  private boolean isInnerClass(Symbol.TypeSymbol symbol) {
    return symbol.owner().isKind(Symbol.TYP);
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {
    private final Symbol.TypeSymbol classSymbol;

    public MethodInvocationVisitor(Symbol.TypeSymbol classSymbol) {
      this.classSymbol = classSymbol;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) tree;
      Symbol symbol = mit.getSymbol();
      if (symbol.isKind(Symbol.MTH) && mit.methodSelect().is(Tree.Kind.IDENTIFIER) && isInherited(symbol)) {
        String methodName = ((IdentifierTree) mit.methodSelect()).name();
        addIssue(tree, "Prefix this call to \""+methodName+"\" with \"super.\".");
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isInherited(Symbol symbol) {
      return new Types().isSubtype(classSymbol.getType(), symbol.owner().getType())
          && classSymbol.getType() != symbol.owner().getType();
    }
  }
}
