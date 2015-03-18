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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2160",
  name = "Subclasses that add fields should override \"equals\"",
  tags = {"bug"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("30min")
public class EqualsNotOverriddenInSubclassCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (hasSemantic() && hasAtLeastOneField(classTree) && !implementsEquals(classTree) && parentClassImplementsEquals(classTree)) {
      addIssue(classTree, "Override this superclass' \"equals\" method.");
    }
  }

  private boolean hasAtLeastOneField(ClassTree classTree) {
    for (Tree member : classTree.members()) {
      if (isField(member)) {
        return true;
      }
    }
    return false;
  }

  private boolean isField(Tree tree) {
    return tree.is(Kind.VARIABLE) && !((VariableTree) tree).modifiers().modifiers().contains(Modifier.STATIC);
  }

  private boolean implementsEquals(ClassTree classTree) {
    return hasNotFinalEqualsMethod(classTree.symbol());
  }

  private boolean parentClassImplementsEquals(ClassTree tree) {
    Tree superClass = tree.superClass();
    if (superClass != null) {
      Type superClassType = ((AbstractTypedTree) superClass).getSymbolType();
      // FIXME Workaround until SONARJAVA-901 is resolved
      while (!superClassType.getSymbol().getType().isTagged(Type.UNKNOWN) && !superClassType.is("java.lang.Object")) {
        TypeSymbol superClassSymbol = superClassType.getSymbol();
        if (hasNotFinalEqualsMethod(superClassSymbol)) {
          return true;
        }
        superClassType = superClassSymbol.getSuperclass();
      }
    }
    return false;
  }

  private boolean hasNotFinalEqualsMethod(Symbol.TypeSymbolSemantic superClassSymbol) {
    for (Symbol symbol : ((TypeSymbol) superClassSymbol).members().lookup("equals")) {
      if (isEqualsMethod(symbol) && !symbol.isFinal()) {
        return true;
      }
    }
    return false;
  }

  private boolean isEqualsMethod(Symbol symbol) {
    if (symbol.isMethodSymbol()) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      return !methodSymbol.getParametersTypes().isEmpty() && methodSymbol.getParametersTypes().get(0).is("java.lang.Object");
    }
    return false;
  }
}
