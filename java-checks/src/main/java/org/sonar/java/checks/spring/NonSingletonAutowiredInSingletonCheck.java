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
import java.util.stream.Stream;
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
  private static final Set<String> SINGLETON_LITERALS = Set.of("singleton", "\"singleton\"");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  /**
   * This rule reports an issue when a Singleton Bean auto-wires a non-Singleton Bean via constructor, field or method parameter.
   */
  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if(isSingletonBean(classTree)) {
      var autowiredBeansStream = Stream.of(
          autowiredClassFields(classTree),
          parametersFromAutowiredMethods(classTree),
          parametersFromAutowiredConstructors(classTree),
          autowiredParametersFromMethodsAndConstructors(classTree)
        )
        .flatMap(stream -> stream)
        // Remove duplicates in case of both method/constructor and parameters are annotated with @Autowired
        .distinct();

      analyzeInjectedBeans(autowiredBeansStream);
    }
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

  private  void analyzeInjectedBeans(Stream<VariableTree> injectedBeans) {
    injectedBeans
      .filter(NonSingletonAutowiredInSingletonCheck::hasNotSingletonBeanType)
      .forEach(variable -> reportIssue(variable.type(), "Singleton beans should not auto-wire non-Singleton beans."));
  }

  private static boolean hasNotSingletonBeanType(VariableTree classField) {
    var typeSymbolDeclaration = classField.type().symbolType().symbol().declaration();
    return typeSymbolDeclaration != null && hasNotSingletonScopeAnnotation(typeSymbolDeclaration.modifiers().annotations());
  }

  private static Stream<VariableTree> autowiredClassFields(ClassTree classTree) {
    return classTree.members()
      .stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(classField -> hasAutowiredAnnotation(classField.modifiers().annotations()));
  }

  private static Stream<VariableTree> parametersFromAutowiredMethods(ClassTree classTree) {
    return classTree.members()
      .stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(method -> hasAutowiredAnnotation(method.modifiers().annotations()))
      .flatMap(methodTree -> methodTree.parameters().stream());
  }

  private static Stream<VariableTree> parametersFromAutowiredConstructors(ClassTree classTree) {
    return classTree.members()
      .stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .filter(NonSingletonAutowiredInSingletonCheck::isAutowiredConstructor)
      .flatMap(methodTree -> methodTree.parameters().stream());
  }

  private static Stream<VariableTree> autowiredParametersFromMethodsAndConstructors(ClassTree classTree) {
    return classTree.members()
      .stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .flatMap(methodTree -> methodTree.parameters().stream())
      .filter(parameter -> hasAutowiredAnnotation(parameter.modifiers().annotations()));
  }

  private static boolean isAutowiredConstructor(MethodTree constructor) {
    return constructor.parameters().size() == 1 || hasAutowiredAnnotation(constructor.modifiers().annotations());
  }

  private static boolean hasAutowiredAnnotation(List<AnnotationTree> annotations) {
    return annotations
      .stream()
      .anyMatch(annotation -> annotation.symbolType().is(AUTOWIRED_ANNOTATION));
  }
}
