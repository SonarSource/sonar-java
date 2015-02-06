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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2134",
  name = "Classes extending java.lang.Thread should override the \"run\" method",
  tags = {"multi-threading", "pitfall"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class ThreadOverridesRunCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTreeImpl classTree = (ClassTreeImpl) tree;
    Symbol.TypeSymbol classSymbol = classTree.getSymbol();
    if (classSymbol != null && classSymbol.getSuperclass().is("java.lang.Thread") && !overridesRunMethod(classSymbol)) {
      addIssue(tree, "Stop extending the Thread class as the \"run\" method is not overridden");
    }
  }

  private boolean overridesRunMethod(Symbol.TypeSymbol classSymbol) {
    List<Symbol> runSymbols = classSymbol.members().lookup("run");
    boolean overridesRunMethod = false;
    for (Symbol run : runSymbols) {
      if (run.isKind(Symbol.MTH)) {
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) run;
        if (methodSymbol.getParametersTypes().isEmpty()) {
          overridesRunMethod = true;
          break;
        }
      }
    }
    return overridesRunMethod;
  }
}
