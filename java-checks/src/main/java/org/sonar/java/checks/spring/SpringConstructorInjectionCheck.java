/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4288")
public class SpringConstructorInjectionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isClassTreeAnnotatedWith(classTree,
      SpringUtils.CONTROLLER_ANNOTATION,
      SpringUtils.REPOSITORY_ANNOTATION,
      SpringUtils.SERVICE_ANNOTATION)) {
      List<Tree> toReport = classTree.members()
        .stream()
        .filter(SpringConstructorInjectionCheck::isMemberAutowired)
        .map(SpringConstructorInjectionCheck::toReportTree)
        .toList();

      if (!toReport.isEmpty()) {
        int cost = toReport.size();
        List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();

        // find constructor
        classTree.members().stream()
          .filter(m -> m.is(Tree.Kind.CONSTRUCTOR))
          .map(m -> ((MethodTree) m).simpleName())
          .findFirst()
          .map(id -> new JavaFileScannerContext.Location("Constructor where you can inject these fields.", id))
          .ifPresent(secondaries::add);

        toReport.stream().skip(1).map(i -> new JavaFileScannerContext.Location("Also remove this annotation.", i)).forEach(secondaries::add);

        reportIssue(toReport.get(0), "Remove this annotation and use constructor injection instead.", secondaries, cost);
      }
    }
  }

  private static boolean isMemberAutowired(Tree member) {
    Symbol s = null;
    if (member.is(Tree.Kind.VARIABLE)) {
      s = ((VariableTree) member).symbol();
    } else if (member.is(Tree.Kind.METHOD)) {
      s = ((MethodTree) member).symbol();
    }
    return s != null && !s.isStatic() && isAutowired(s);
  }

  private static boolean isAutowired(Symbol s) {
    return s.metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION);
  }

  private static Tree toReportTree(Tree member) {
    Stream<AnnotationTree> stream = Stream.empty();
    if (member.is(Tree.Kind.VARIABLE)) {
      stream = ((VariableTree) member).modifiers().annotations().stream();
    } else if (member.is(Tree.Kind.METHOD)) {
      stream = ((MethodTree) member).modifiers().annotations().stream();
    }
    return stream
      .filter(a -> a.annotationType().symbolType().is(SpringUtils.AUTOWIRED_ANNOTATION))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Mapping a tree to something unexpected"));
  }

  private static boolean isClassTreeAnnotatedWith(ClassTree classTree, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }
}
