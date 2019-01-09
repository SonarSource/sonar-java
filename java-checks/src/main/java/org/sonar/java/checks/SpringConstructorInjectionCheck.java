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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4288")
public class SpringConstructorInjectionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (isClassTreeAnnotatedWith(classTree,
      "org.springframework.stereotype.Controller",
      "org.springframework.stereotype.Repository",
      "org.springframework.stereotype.Service")) {
      List<Tree> toReport = classTree.members()
        .stream()
        .filter(SpringConstructorInjectionCheck::isMemberAutowired)
        .map(SpringConstructorInjectionCheck::toReportTree)
        .collect(Collectors.toList());

      if (!toReport.isEmpty()) {
        int cost = toReport.size();
        // find constructor
        classTree.members().stream()
          .filter(m -> m.is(Kind.CONSTRUCTOR))
          .map(m -> ((MethodTree) m).simpleName())
          .findFirst()
          .ifPresent(toReport::add);

        reportIssue(toReport.get(0),
          "Remove this annotation and use constructor injection instead.",
          toReport.stream().skip(1).map(i -> new JavaFileScannerContext.Location("", i)).collect(Collectors.toList())
          , cost);
      }
    }
  }

  private static boolean isMemberAutowired(Tree member) {
    Symbol s = null;
    if (member.is(Kind.VARIABLE)) {
      s = ((VariableTree) member).symbol();
    } else if (member.is(Kind.METHOD)) {
      s = ((MethodTree) member).symbol();
    }
    return s != null && !s.isStatic() && isAutowired(s);
  }

  private static boolean isAutowired(Symbol s) {
    return s.metadata().isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");
  }

  private static Tree toReportTree(Tree member) {
    Stream<AnnotationTree> stream = Stream.empty();
    if (member.is(Kind.VARIABLE)) {
      stream = ((VariableTree) member).modifiers().annotations().stream();
    } else if (member.is(Kind.METHOD)) {
      stream = ((MethodTree) member).modifiers().annotations().stream();
    }
    return stream
      .filter(a -> a.annotationType().symbolType().is("org.springframework.beans.factory.annotation.Autowired"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Mapping a tree to something unexpected"));
  }

  private static boolean isClassTreeAnnotatedWith(ClassTree classTree, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }
}
