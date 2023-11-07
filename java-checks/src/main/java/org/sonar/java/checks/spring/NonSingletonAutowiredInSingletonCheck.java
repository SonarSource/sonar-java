/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6832")
public class NonSingletonAutowiredInSingletonCheck extends IssuableSubscriptionVisitor {
  private static final String SCOPED_ANNOTATION = "org.springframework.context.annotation.Scope";
  private static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";
  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";
  private static final String JAKARTA_INJECT_ANNOTATION = "jakarta.inject.Inject";
  private static final Set<String> AUTO_WIRING_ANNOTATIONS = Set.of(AUTOWIRED_ANNOTATION, JAVAX_INJECT_ANNOTATION, JAKARTA_INJECT_ANNOTATION);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION, Tree.Kind.CONSTRUCTOR);
  }

  /**
   * This rule reports an issue when a Singleton Bean auto-wires a non-Singleton Bean via constructor, field or method parameter.
   */
  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.ANNOTATION)) {
      analyzeAnnotation((AnnotationTree) tree);
    }

    if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      analyzeSingleArgumentConstructor((MethodTree) tree);
    }
  }

  private void analyzeAnnotation(AnnotationTree annotationTree) {
    if (!isAutoWiringAnnotation(annotationTree)) {
      return;
    }

    var annotatedSymbol = Optional.ofNullable(annotationTree.parent()).map(Tree::parent).orElse(null);
    if (annotatedSymbol == null) {
      return;
    }

    if (annotatedSymbol.is(Tree.Kind.VARIABLE)) {
      analyzeAnnotatedFieldOrParameter((VariableTree) annotatedSymbol);

    } else if (annotatedSymbol.is(Tree.Kind.METHOD)) {
      analyzeAnnotatedSetter((MethodTree) annotatedSymbol);

    } else if (annotatedSymbol.is(Tree.Kind.CONSTRUCTOR)) {
      analyzeAnnotatedConstructor((MethodTree) annotatedSymbol);
    }
  }

  private void analyzeAnnotatedFieldOrParameter(VariableTree annotatedVar) {
    String injectionType;

    if (isClassField(annotatedVar)) {
      injectionType = "autowired field";
    } else if (isSetterParameter(annotatedVar) || isConstructorParameter(annotatedVar)) {
      injectionType = "autowired parameter";
    } else {
      injectionType = null;
    }

    if (injectionType != null) {
      getEnclosingClass(annotatedVar.symbol().enclosingClass())
        .ifPresent(enclosingClassTree -> reportIfNonSingletonInSingleton(enclosingClassTree, annotatedVar, injectionType));
    }
  }

  private void analyzeAnnotatedSetter(MethodTree annotatedMethod) {
    if (MethodTreeUtils.isSetterMethod(annotatedMethod)) {
      getEnclosingClass(annotatedMethod.symbol().enclosingClass())
        .ifPresent(enclosingClassTree -> annotatedMethod.parameters()
          .forEach(variableTree -> reportIfNonSingletonInSingleton(enclosingClassTree, variableTree, "autowired setter method")));
    }
  }

  private void analyzeAnnotatedConstructor(MethodTree annotatedConstructor) {
    getEnclosingClass(annotatedConstructor.symbol().enclosingClass())
      .ifPresent(enclosingClassTree -> annotatedConstructor.parameters()
        .forEach(variableTree -> reportIfNonSingletonInSingleton(enclosingClassTree, variableTree, "autowired constructor")));
  }

  private void analyzeSingleArgumentConstructor(MethodTree constructorTree) {
    if (constructorTree.parameters().size() == 1) {
      var constructorParameter = constructorTree.parameters().get(0);
      getEnclosingClass(constructorTree.symbol().enclosingClass())
        .ifPresent(enclosingClassTree -> reportIfNonSingletonInSingleton(enclosingClassTree, constructorParameter, "single argument constructor"));
    }
  }

  private static boolean isClassField(VariableTree variableTree) {
    return Optional.ofNullable(variableTree.parent())
      .filter(parent -> parent.is(Tree.Kind.CLASS))
      .isPresent();
  }

  private static boolean isSetterParameter(VariableTree variableTree) {
    return Optional.ofNullable(variableTree.parent())
      .filter(parent -> parent.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(MethodTreeUtils::isSetterMethod)
      .isPresent();
  }

  private static boolean isConstructorParameter(VariableTree variableTree) {
    return Optional.ofNullable(variableTree.parent())
      .filter(parent -> parent.is(Tree.Kind.CONSTRUCTOR))
      .isPresent();
  }

  private static Optional<ClassTree> getEnclosingClass(@Nullable Symbol.TypeSymbol enclosingClassSymbol) {
    return Optional.ofNullable(enclosingClassSymbol).map(Symbol::declaration).map(ClassTree.class::cast);
  }

  private void reportIfNonSingletonInSingleton(ClassTree enclosingClassTree, VariableTree variableTree, String injectionType) {
    if (isSingletonBean(enclosingClassTree) && hasTypeNotSingletonBean(variableTree)) {
      reportIssue(variableTree.type(), "Don't auto-wire this non-Singleton bean into a Singleton bean (" + injectionType + ").");
    }
  }

  private static boolean hasTypeNotSingletonBean(VariableTree variableTree) {
    return hasNotSingletonScopeAnnotation(variableTree.symbol().type().symbol().metadata().annotations());
  }

  private static boolean isAutoWiringAnnotation(AnnotationTree annotationTree) {
    return AUTO_WIRING_ANNOTATIONS.contains(annotationTree.symbolType().fullyQualifiedName());
  }

  private static boolean isSingletonBean(ClassTree classTree) {
    return !hasNotSingletonScopeAnnotation(classTree.symbol().metadata().annotations());
  }

  private static boolean hasNotSingletonScopeAnnotation(List<SymbolMetadata.AnnotationInstance> annotations) {
    // Only classes annotated with @Scope, having a value different from "singleton", are considered as non-Singleton
    return annotations.stream().anyMatch(NonSingletonAutowiredInSingletonCheck::isNotSingletonScopeAnnotation);
  }

  private static boolean isNotSingletonScopeAnnotation(SymbolMetadata.AnnotationInstance annotationInstance) {
    return annotationInstance.symbol().type().is(SCOPED_ANNOTATION)
      && annotationInstance.values()
        .stream()
        .anyMatch(NonSingletonAutowiredInSingletonCheck::isNotSingletonAnnotationValue);
  }

  private static boolean isNotSingletonAnnotationValue(SymbolMetadata.AnnotationValue annotationValue) {
    return ("value".equals(annotationValue.name()) || "scopeName".equals(annotationValue.name()))
      // both "value" and "scopeName" in @Scope annotation have String type
      && !"singleton".equalsIgnoreCase((String) annotationValue.value());
  }
}
