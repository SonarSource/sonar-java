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

import java.util.ArrayList;
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

//https://github.com/SonarSource/sonar-java/pull/5455

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
    if (membersMainMethods.isEmpty()) {
      return;
    }
    if (membersMainMethods.size() > 1) {
      reportIssue(membersMainMethods.get(0).simpleName(), "At most one main method should be defined in a class.");
      return;
    }
    List<MethodTree> superMainMethods = findMainMethodsInSuperclasses(ct);
    if (superMainMethods.isEmpty()) {
      return;
    }

    // at this point : 1 main method in members and at least 1 main method in superclasses
    var singleMainMethod = membersMainMethods.get(0);
    boolean isOverriding = Optional.ofNullable(singleMainMethod.isOverriding()).orElse(false);

    // override case
    var mainWithHigherPriorityInSuper = superMainMethods.stream().filter(superMainMethod ->
      MethodTreeUtils.compareMainMethodPriority(singleMainMethod, superMainMethod) < 0
    ).findFirst();
    mainWithHigherPriorityInSuper.ifPresentOrElse(
      // there is a main method in superclasses with higher priority than the one in members, so the one in members will not be the entry point
      superMainMethod -> reportIssue(
        singleMainMethod.simpleName(),
        "This 'main' method will not be the entry point because another inherited 'main' from %s takes precedence."
          .formatted(enclosingClassName(superMainMethod))
      ),
      // there is no main method in superclasses with higher priority than the one in members, so the one in members will be the entry point, but if it is not overriding, it is
      // a problem as it introduces multiple main methods
      () -> {
        if (!isOverriding) {
          var superMainMethod = superMainMethods.get(0);
          reportIssue(
            singleMainMethod,
            "Override main from %s to avoid introducing multiple main methods."
              .formatted(enclosingClassName(superMainMethod))
          );
        }
      }
    );
  }

  private String enclosingClassName(MethodTree mainMethod) {
    var enclosingClass = mainMethod.symbol().enclosingClass();
    return enclosingClass == null ? "unknown" : enclosingClass.name();
  }

  private List<MethodTree> findMainMethodsInSuperclasses(ClassTree ct) {
    List<MethodTree> mains = new ArrayList<>();
    var superClass = ct.superClass();
    while (superClass != null) {
      var superClassTree = superClass.symbolType().symbol().declaration();
      findMainMethodsInMembers(superClassTree)
        .forEach(mains::add);
      superClass = superClassTree.superClass();
    }
    return mains;
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
