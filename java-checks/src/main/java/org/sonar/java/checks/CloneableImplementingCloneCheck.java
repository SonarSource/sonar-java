/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2157")
public class CloneableImplementingCloneCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher CLONE_MATCHER = MethodMatcher.create().name("clone").withoutParameter();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isCloneable(classTree) && !classSymbol.isAbstract() && !declaresCloneMethod(classSymbol)) {
      reportIssue(classTree.simpleName(), "Add a \"clone()\" method to this class.");
    }
  }

  private static boolean declaresCloneMethod(Symbol.TypeSymbol classSymbol) {
    return classSymbol.lookupSymbols("clone").stream().anyMatch(CLONE_MATCHER::matches);
  }

  private static boolean isCloneable(ClassTree classTree) {
    return classTree.superInterfaces().stream().map(TypeTree::symbolType).anyMatch(t -> t.is("java.lang.Cloneable"));
  }
}
