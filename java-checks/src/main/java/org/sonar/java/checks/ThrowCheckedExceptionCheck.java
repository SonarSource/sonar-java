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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
    key = "S1162",
    priority = Priority.MAJOR,
    tags = {"error-handling"})
public class ThrowCheckedExceptionCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.THROW_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ThrowStatementTree throwStatementTree = (ThrowStatementTree) tree;
    Type symbolType = ((AbstractTypedTree) throwStatementTree.expression()).getSymbolType();
    //do not handle unknown symbols.
    if (symbolType.isTagged(Type.CLASS) && isCheckedException((Type.ClassType) symbolType)) {
      addIssue(tree, "Remove the usage of the checked exception '"+symbolType.getSymbol().getName()+"'.");
    }
  }

  private boolean isCheckedException(Type.ClassType symbolType) {
    Type.ClassType superType = symbolType;
    while (superType != null) {
      if (superType.is("java.lang.RuntimeException")) {
        return false;
      }
      if (superType.is("java.lang.Exception")) {
        return true;
      }
      superType = (Type.ClassType) superType.getSymbol().getSuperclass();
    }
    return false;
  }
}
