/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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

@Rule(key = "S1206")
public class EqualsOverriddenWithHashCodeCheck extends IssuableSubscriptionVisitor {

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
      .map(MethodTree.class::cast)
      .toList();

    Optional<MethodTree> equalsMethod = methods.stream().filter(EqualsOverriddenWithHashCodeCheck::isEquals).findAny();
    Optional<MethodTree> hashCodeMethod = methods.stream().filter(EqualsOverriddenWithHashCodeCheck::isHashCode).findAny();

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

  private static String getMessage(String overriddenMethod, String methodToOverride) {
    return "This class overrides \"" + overriddenMethod + "()\" and should therefore also override \"" + methodToOverride + "()\".";
  }

}
