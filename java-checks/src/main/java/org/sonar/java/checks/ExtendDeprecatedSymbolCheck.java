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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S1724",
  name = "Deprecated classes and interfaces should not be extended/implemented",
  tags = {"cwe", "obsolete"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("30min")
public class ExtendDeprecatedSymbolCheck extends AbstractDeprecatedChecker {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!hasDeprecatedAnnotation(tree)) {
      checkSuperTypeDeprecation(classTree.superClass(), false);
      for (TypeTree superInterface : classTree.superInterfaces()) {
        checkSuperTypeDeprecation(superInterface, true);
      }
    }
  }

  private void checkSuperTypeDeprecation(@Nullable TypeTree superTypeTree, boolean isInterface) {
    if (superTypeTree != null) {
      Type symbolType = superTypeTree.symbolType();
      if (symbolType.isClass() && symbolType.symbol().isDeprecated()) {
        addIssue(superTypeTree, "\"" + symbolType.symbol().name() + "\"" + " is deprecated, "
          + (isInterface ? "implement" : "extend") + " the suggested replacement instead.");
      }
    }
  }

}
