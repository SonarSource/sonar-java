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
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
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
  private static final Set<String> SINGLETON_LITERALS = Set.of("singleton", "\"singleton\"");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION, Tree.Kind.CONSTRUCTOR);
  }

  /**
   * This rule reports an issue when a Singleton Bean auto-wires a non-Singleton Bean via constructor, field or method parameter.
   */
  @Override
  public void visitNode(Tree tree) {
    if(tree.is(Tree.Kind.ANNOTATION)) {
      var annotationTree = (AnnotationTree) tree;

      if(isAutoWiringAnnotation(annotationTree) && isAnnotationOnKind(annotationTree, Tree.Kind.VARIABLE)) {
        var variableTree = (VariableTree) annotationTree.parent().parent();
        var enclosingClassTree = variableTree.symbol().enclosingClass().declaration();
        reportIfNonSingletonInSingleton(enclosingClassTree, variableTree, "autowired field/parameter");

      } else if(isAutoWiringAnnotation(annotationTree)
        && (isAnnotationOnKind(annotationTree, Tree.Kind.METHOD) || isAnnotationOnKind(annotationTree, Tree.Kind.CONSTRUCTOR))) {
        var methodTree = (MethodTree) annotationTree.parent().parent();
        var enclosingClassTree = methodTree.symbol().enclosingClass().declaration();
        methodTree.parameters()
          .forEach(variableTree -> reportIfNonSingletonInSingleton(enclosingClassTree, variableTree, "autowired method/constructor"));
      }
    }

    if(tree.is(Tree.Kind.CONSTRUCTOR)) {
      var constructorTree = (MethodTree) tree;
      var enclosingClassTree = constructorTree.symbol().enclosingClass().declaration();

      if(constructorTree.parameters().size() == 1) {
        reportIfNonSingletonInSingleton(enclosingClassTree, constructorTree.parameters().get(0), "single argument constructor");
      }
    }
  }

  private void reportIfNonSingletonInSingleton(ClassTree enclosingClassTree, VariableTree variableTree, String injectionType) {
    if(isSingletonBean(enclosingClassTree) && hasTypeNotSingletonBean(variableTree)) {
      reportIssue(variableTree.type(), "Don't auto-wire this non-Singleton bean into a Singleton bean (" + injectionType + ").");
    }
  }

  private static boolean isAnnotationOnKind(AnnotationTree annotationTree, Tree.Kind kind) {
    return annotationTree.parent().parent().is(kind);
  }

  private static boolean hasTypeNotSingletonBean(VariableTree variableTree) {
    var typeSymbolDeclaration = variableTree.type().symbolType().symbol().declaration();
    return typeSymbolDeclaration != null && hasNotSingletonScopeAnnotation(typeSymbolDeclaration.modifiers().annotations());
  }

  private static boolean isAutoWiringAnnotation(AnnotationTree annotationTree) {
    return AUTO_WIRING_ANNOTATIONS.contains(annotationTree.symbolType().fullyQualifiedName());
  }

  private static boolean isSingletonBean(ClassTree classTree) {
    return !hasNotSingletonScopeAnnotation(classTree.modifiers().annotations());
  }

  private static boolean hasNotSingletonScopeAnnotation(List<AnnotationTree> annotations) {
    // Only classes annotated with @Scope, having a value different from "singleton", are considered as non-Singleton
    return annotations.stream().anyMatch(NonSingletonAutowiredInSingletonCheck::isNotSingletonScopeAnnotation);
  }

  private static boolean isNotSingletonScopeAnnotation(AnnotationTree annotationTree) {
    return annotationTree.symbolType().is(SCOPED_ANNOTATION)
      && (isNotSingletonLiteralValue(annotationTree.arguments()) || isNotSingletonAssignmentValue(annotationTree.arguments()));
  }

  private static boolean isNotSingletonLiteralValue(Arguments arguments){
    return arguments.size() == 1
      && arguments.get(0).is(Tree.Kind.STRING_LITERAL)
      && isNotSingletonLiteral(((LiteralTree) arguments.get(0)).value());
  }

  private static boolean isNotSingletonAssignmentValue(Arguments arguments) {
    return arguments
      .stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .anyMatch(NonSingletonAutowiredInSingletonCheck::isNotAssignmentToSingletonValue);
  }

  private static boolean isNotAssignmentToSingletonValue(AssignmentExpressionTree assignmentExpressionTree) {
    var expression = assignmentExpressionTree.expression();

    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return isNotSingletonLiteral(((LiteralTree) expression).value());
    }

    var variable = (IdentifierTree) assignmentExpressionTree.variable();

    return ("value".equals(variable.name()) || "scopeName".equals(variable.name()))
      && (expression.is(Tree.Kind.MEMBER_SELECT) && isNotSingletonLiteral(((MemberSelectExpressionTree) expression).identifier().name()));
  }

  private static boolean isNotSingletonLiteral(String value) {
    return SINGLETON_LITERALS.stream().noneMatch(singletonLiteral -> singletonLiteral.equalsIgnoreCase(value));
  }
}
