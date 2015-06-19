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
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(
  key = "RedundantThrowsDeclarationCheck",
  name = "Throws declarations should not be superfluous",
  tags = {"error-handling", "security"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class RedundantThrowsDeclarationCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    List<TypeTree> exceptionsTree = new ArrayList<>(((MethodTree) tree).throwsClauses());
    checkRuntimeExceptions(tree, exceptionsTree);
    checkRedundantExceptions(tree, exceptionsTree);
    checkRelatedExceptions(tree, exceptionsTree);
  }

  private void checkRuntimeExceptions(Tree tree, List<TypeTree> exceptionsTree) {
    for (int i = exceptionsTree.size() - 1; i >= 0; i--) {
      TypeTree exceptionTree = exceptionsTree.get(i);
      Type exceptionType = exceptionTree.symbolType();
      if (exceptionType.isSubtypeOf("java.lang.RuntimeException")) {
        addIssue(tree, "Remove the declaration of thrown exception '" + exceptionType.fullyQualifiedName() + "' which is a runtime exception.");
        exceptionsTree.remove(i);
      }
    }
  }

  private void checkRedundantExceptions(Tree tree, List<TypeTree> exceptionsTree) {
    for (int i1 = exceptionsTree.size() - 1; i1 >= 0; i1--) {
      TypeTree exceptionTree = exceptionsTree.get(i1);
      Type exceptionType = exceptionTree.symbolType();
      for (int i2 = i1 - 1; i2 >= 0; i2--) {
        Type secondExceptionType = exceptionsTree.get(i2).symbolType();
        if (exceptionType.equals(secondExceptionType) && !exceptionType.symbol().equals(Symbols.unknownSymbol)) {
          addIssue(tree, "Remove the redundant '" + exceptionType.fullyQualifiedName() + "' thrown exception declaration(s).");
          exceptionsTree.remove(i1);
          break;
        }
      }
    }
  }

  private void checkRelatedExceptions(Tree tree, List<TypeTree> exceptionsTree) {
    for (int i1 = exceptionsTree.size() - 1; i1 >= 0; i1--) {
      TypeTree exceptionTree = exceptionsTree.get(i1);
      Type exceptionType = exceptionTree.symbolType();
      for (int i2 = exceptionsTree.size() - 1; i2 >= 0; i2--) {
        Type otherExceptionType = exceptionsTree.get(i2).symbolType();
        if (i1 != i2 && exceptionType.isSubtypeOf(otherExceptionType)) {
          addIssue(tree, "Remove the declaration of thrown exception '" + exceptionType.fullyQualifiedName() + "' which is a subclass of '" +
            otherExceptionType.fullyQualifiedName() + "'.");
          break;
        }
      }
    }
  }

}
