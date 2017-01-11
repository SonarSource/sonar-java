/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2078")
public class LDAPInjectionCheck extends AbstractInjectionChecker {

  private static final MethodMatcher LDAP_SEARCH_MATCHER = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("javax.naming.directory.DirContext"))
    .name("search").withAnyParameters();

  private static final MethodMatcher SEARCH_CONTROLS_MATCHER = MethodMatcher.create()
    .typeDefinition("javax.naming.directory.SearchControls")
    .name("setReturningAttributes").addParameter("java.lang.String[]");

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (isDirContextSearchCall(mit)) {
      // Check the first two arguments of search method
      checkDirContextArg(mit.arguments().get(0), mit);
      checkDirContextArg(mit.arguments().get(1), mit);
    } else if (isSearchControlCall(mit)) {
      ExpressionTree arg = mit.arguments().get(0);
      if (isDynamicArray(arg, mit)) {
        reportIssue(arg, "Make sure that \"" + parameterName + "\" is sanitized before use in this LDAP request.");
      }
    }
  }

  private void checkDirContextArg(ExpressionTree arg1, MethodInvocationTree mit) {
    if (arg1.symbolType().is("java.lang.String") && isDynamicString(mit, arg1, null)) {
      reportIssue(arg1, "Make sure that \"" + parameterName + "\" is sanitized before use in this LDAP request.");
    }
  }

  private boolean isDynamicArray(ExpressionTree arg, MethodInvocationTree mit) {
    if (arg.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree nat = (NewArrayTree) arg;
      for (ExpressionTree expressionTree : nat.initializers()) {
        if (isDynamicString(mit, expressionTree, null)) {
          return true;
        }
      }
      return false;
    }
    setParameterNameFromArgument(arg);
    return true;
  }

  private boolean isDirContextSearchCall(MethodInvocationTree methodTree) {
    return hasSemantic() && LDAP_SEARCH_MATCHER.matches(methodTree);
  }

  private boolean isSearchControlCall(MethodInvocationTree methodTree) {
    return hasSemantic() && SEARCH_CONTROLS_MATCHER.matches(methodTree);
  }

}
