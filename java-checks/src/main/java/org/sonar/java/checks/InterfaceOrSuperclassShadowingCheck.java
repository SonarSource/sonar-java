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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2176",
  name = "Class names should not shadow interfaces or superclasses",
  tags = {"pitfall"},
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation(value = "5min")
public class InterfaceOrSuperclassShadowingCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTreeImpl classTree = (ClassTreeImpl) tree;
    if (hasSemantic()) {
      TypeSymbol classSymbol = classTree.getSymbol();
      checkSuperType(classTree, classSymbol.getSuperclass());
      for (Type interfaceType : classSymbol.getInterfaces()) {
        checkSuperType(classTree, interfaceType);
      }
    }
  }

  private void checkSuperType(ClassTreeImpl tree, Type superType) {
    if (superType != null && superType.getSymbol().getName().equals(tree.getSymbol().getName())) {
      String classOrInterface = tree.is(Tree.Kind.CLASS) ? "class" : "interface";
      addIssue(tree, "Rename this " + classOrInterface + ".");
    }
  }

}
