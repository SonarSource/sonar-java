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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Rule(key = "S1206")
public class EqualsOverridenWithHashCodeCheck extends IssuableSubscriptionVisitor {

  private static final String HASHCODE = "hashCode";
  private static final String EQUALS = "equals";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    List<MethodTree> methods = ((ClassTree) tree).members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(member -> (MethodTree) member)
      .collect(Collectors.toList());

    Optional<MethodTree> equalsMethod = methods.stream().filter(EqualsOverridenWithHashCodeCheck::isEquals).findAny();
    Optional<MethodTree> hashCodeMethod = methods.stream().filter(EqualsOverridenWithHashCodeCheck::isHashCode).findAny();

    if (equalsMethod.isPresent() && !hashCodeMethod.isPresent()) {
      reportIssue(equalsMethod.get().simpleName(), getMessage(EQUALS, HASHCODE));
    } else if (hashCodeMethod.isPresent() && !equalsMethod.isPresent()) {
      reportIssue(hashCodeMethod.get().simpleName(), getMessage(HASHCODE, EQUALS));
    }
  }

  private static boolean isEquals(MethodTree methodTree) {
    return MethodTreeUtils.isEqualsMethod(methodTree);
  }

  private static boolean isHashCode(MethodTree methodTree) {
    return HASHCODE.equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty() && returnsInt(methodTree);
  }

  private static boolean returnsInt(MethodTree tree) {
    TypeTree typeTree = tree.returnType();
    return typeTree != null && typeTree.symbolType().isPrimitive(org.sonar.plugins.java.api.semantic.Type.Primitives.INT);
  }

  private static String getMessage(String overridenMethod, String methodToOverride) {
    return "This class overrides \"" + overridenMethod + "()\" and should therefore also override \"" + methodToOverride + "()\".";
  }

}
