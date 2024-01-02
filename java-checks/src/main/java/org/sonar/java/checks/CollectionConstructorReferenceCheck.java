/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5329")
public class CollectionConstructorReferenceCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes(
        "java.util.ArrayList",
        "java.util.HashMap",
        "java.util.HashSet",
        "java.util.Hashtable",
        "java.util.IdentityHashMap",
        "java.util.LinkedHashMap",
        "java.util.LinkedHashSet",
        "java.util.PriorityQueue",
        "java.util.Vector",
        "java.util.WeakHashMap")
      .constructor()
      .addParametersMatcher("int")
      .build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_REFERENCE);
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReference) {
    if ("java.util.function.Function".equals(methodReference.symbolType().fullyQualifiedName())) {
      String methodOwnerTypeName = ((ExpressionTree) methodReference.expression()).symbolType().name();
      reportIssue(methodReference, String.format(
        "Replace this method reference by a lambda to explicitly show the usage of %1$s(int %2$s) or %1$s().",
        methodOwnerTypeName,
        "IdentityHashMap".equals(methodOwnerTypeName) ? "expectedMaxSize" : "initialCapacity"));
    }
  }

}
