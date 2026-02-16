/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8446")
public class MultipleMainInstancesCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.RECORD, Tree.Kind.IMPLICIT_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree ct = (ClassTree) tree;
    List<MethodTree> membersMainMethods = findMainMethodsInMembers(ct).toList();
    List<MethodTree> superMainMethods = findMainMethodsInSuperclasses(ct).toList();
    boolean hasMembersMainMethod = !membersMainMethods.isEmpty();
    boolean hasMultipleMainMethods = membersMainMethods.size() + superMainMethods.size() > 1;
    boolean hasLegitSingleMainOverride =
      membersMainMethods.size() == 1
        && Optional.ofNullable(membersMainMethods.get(0).isOverriding()).orElse(false);

    if (hasMembersMainMethod && hasMultipleMainMethods && !hasLegitSingleMainOverride) {
      var firstMainMethod = membersMainMethods.get(0);
      var firstMainMethodToken = firstMainMethod.simpleName();
      var errorMessage = membersMainMethods.size() > 1 ?
        "At most one main method should be defined in a class." :
        "Main method should not be defined in a class if a main method is already defined in a superclass.";
      reportIssue(firstMainMethodToken, errorMessage);
    }
  }

  private Stream<MethodTree> findMainMethodsInSuperclasses(ClassTree ct) {
    var superClass = ct.superClass();
    if (superClass == null) {
      return Stream.empty();
    }
    var superClassTree = superClass.symbolType().symbol().declaration();
    if (superClassTree == null) {
      return Stream.empty();
    }
    return Stream.concat(
      findMainMethodsInMembers(superClassTree),
      findMainMethodsInSuperclasses(superClassTree)
    );
  }

  private Stream<MethodTree> findMainMethodsInMembers(ClassTree ct) {
    return ct.members().stream()
      .filter(MethodTree.class::isInstance)
      .map(MethodTree.class::cast)
      .filter(this::isMainMethod);
  }

  private boolean isMainMethod(MethodTree tree) {
    return MethodTreeUtils.isMainMethod(tree, context.getJavaVersion());
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }
}
