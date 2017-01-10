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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1161")
public class OverrideAnnotationCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTreeImpl methodTree = (MethodTreeImpl) tree;
    if (isOverriding(methodTree) && !methodTree.isAnnotatedOverride() && !isExcluded(context.getJavaVersion(), methodTree)) {
      reportIssue(methodTree.simpleName(), "Add the \"@Override\" annotation above this method signature");
    }
  }

  private static boolean isOverriding(MethodTreeImpl methodTree) {
    return BooleanUtils.isTrue(methodTree.isOverriding());
  }

  private static boolean isExcluded(JavaVersion javaVersion, MethodTreeImpl methodTree) {
    if (javaVersion.isNotSet()) {
      return false;
    }
    int javaIntVersion = javaVersion.asInt();
    return javaIntVersion <= 4 || (javaIntVersion == 5 && (methodTree.symbol().owner().isInterface() || overrideFromInterface(methodTree)));
  }

  private static boolean overrideFromInterface(MethodTree methodTree) {
    return ((JavaSymbol.MethodJavaSymbol) methodTree.symbol()).overriddenSymbol().owner().isInterface();
  }

}
