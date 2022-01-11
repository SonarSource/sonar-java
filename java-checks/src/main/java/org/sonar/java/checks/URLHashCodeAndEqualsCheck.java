/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2112")
public class URLHashCodeAndEqualsCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_NET_URL = "java.net.URL";

  private static final MethodMatchers URL_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_NET_URL).names("equals").addParametersMatcher("java.lang.Object").build(),
    MethodMatchers.create().ofTypes(JAVA_NET_URL).names("hashCode").addWithoutParametersMatcher().build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) tree;
      if (variableTree.initializer() != null) {
        Type variableType = variableTree.type().symbolType();
        if (isSubTypeOfSetOrMap(variableType) && usesURLAsTypeParameter(variableType)) {
          reportIssue(variableTree.type(), "Use the URI class instead.");
        }
      }
    } else if (URL_MATCHERS.matches((MethodInvocationTree) tree)) {
      reportIssue(tree, "Use the URI class instead.");
    }
  }

  private static boolean isSubTypeOfSetOrMap(Type type) {
    return type.isSubtypeOf("java.util.Set") || type.isSubtypeOf("java.util.Map");
  }

  private static boolean usesURLAsTypeParameter(Type type) {
    Type firstTypeParameter = getFirstTypeParameter(type);
    return firstTypeParameter != null && firstTypeParameter.is(JAVA_NET_URL);
  }

  @CheckForNull
  private static Type getFirstTypeParameter(Type type) {
    if (type.isParameterized()) {
      return type.typeArguments().get(0);
    }
    return null;
  }

}
