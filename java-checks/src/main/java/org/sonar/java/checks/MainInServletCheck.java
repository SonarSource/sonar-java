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

import com.google.common.collect.ImmutableSet;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Rule(key = "S2653")
public class MainInServletCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> SERVLET_AND_EJB_CLASSES = ImmutableSet.of(
    "javax.servlet.http.HttpServlet",
    "org.apache.struts.action.Action",
    "javax.ejb.EnterpriseBean"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree node = (ClassTree) tree;
    Symbol.TypeSymbol symbol = node.symbol();
    if (isServletOrEjb(symbol)) {
      for (Tree member : node.members()) {
        if (member.is(Tree.Kind.METHOD) && MethodTreeUtils.isMainMethod((MethodTree) member)) {
          reportIssue(((MethodTree) member).simpleName(), "Remove this unwanted \"main\" method.");
        }
      }
    }
  }

  private static boolean isServletOrEjb(Symbol symbol) {
    if (SERVLET_AND_EJB_CLASSES.stream().anyMatch(symbol.type()::isSubtypeOf)) {
      return true;
    }
    return symbol.metadata().annotations().stream().anyMatch(annotation -> annotation.symbol().type().fullyQualifiedName().startsWith("javax.ejb."));
  }

}
