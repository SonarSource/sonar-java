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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S7178")
public class StaticFieldInjectionNotSupportedCheck extends IssuableSubscriptionVisitor {
  private static final Set<String> INJECTIONS_ANNOTATIONS = Set.of(
    "javax.inject.Inject",
    "org.springframework.beans.factory.annotation.Autowired",
    "jakarta.inject.Inject",
    "org.springframework.beans.factory.annotation.Value"
  );

  private static final String STATIC_FIELD_MESSAGE = "Remove this injection annotation targeting the static field.";
  private static final String STATIC_METHOD_MESSAGE = "Remove this injection annotation targeting the static method.";
  private static final String STATIC_PARAMETER_MESSAGE = "Remove this injection annotation targeting the parameter.";
  private static final String SPRING_PREFIX = "org.springframework";

  private boolean analyzingSpringProject = false;

  @Override
  public void setContext(JavaFileScannerContext context){
    super.setContext(context);
    // by default, it is not a spring context
    analyzingSpringProject = false;
  }


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD, Tree.Kind.CLASS, Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof CompilationUnitTree compilationUnit) {
      analyzingSpringProject = compilationUnit.imports().stream()
        .filter(ImportTree.class::isInstance)
        .map(ImportTree.class::cast)
        .anyMatch(i -> ExpressionsHelper.concatenate((ExpressionTree) i.qualifiedIdentifier()).startsWith(SPRING_PREFIX));
    }

    if(!analyzingSpringProject){
      return;
    }

    if (tree instanceof MethodTree method && method.symbol().isStatic()) {
      //report on method annotations
      selectInjectionAnnotations(method.modifiers())
        .forEach(ann -> reportIssue(ann, STATIC_METHOD_MESSAGE));

      //report on parameters
      method.parameters().stream()
        .map(p -> selectInjectionAnnotations(p.modifiers()))
        .forEach(anns -> anns.forEach(ann -> reportIssue(ann, STATIC_PARAMETER_MESSAGE)));
    } else if (tree instanceof ClassTree clazz) {
      Stream<VariableTree> staticFields = clazz.members().stream()
        .filter(child -> child instanceof VariableTree v && v.symbol().isStatic())
        .map(VariableTree.class::cast);

      staticFields
        .map(v -> selectInjectionAnnotations(v.modifiers()))
        .forEach(anns ->
          anns.forEach(ann -> reportIssue(ann, STATIC_FIELD_MESSAGE))
        );
    }
  }

  private static List<AnnotationTree> selectInjectionAnnotations(ModifiersTree m) {
    return m.annotations().stream()
      .filter(ann ->
        INJECTIONS_ANNOTATIONS.contains(ann.annotationType().symbolType().fullyQualifiedName()))
      .toList();
  }
}
