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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5128")
public class MissingBeanValidationCheck extends IssuableSubscriptionVisitor {
  private static final List<String> JSR380_VALID_ANNOTATIONS = List.of("javax.validation.Valid", "jakarta.validation.Valid");
  private static final List<String> JSR380_CONSTRAINTS = List.of("javax.validation.Constraint", "jakarta.validation.Constraint");
  private static final List<String> JSR380_CONSTRAINT_VALIDATORS = List.of("javax.validation.ConstraintValidator", "jakarta.validation.ConstraintValidator");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        checkField((VariableTree) member);
      } else if (member.is(Tree.Kind.METHOD)) {
        checkMethod((MethodTree) member);
      }
    }
  }

  private void checkField(VariableTree field) {
    getIssueMessage(field).ifPresent(message -> reportIssue(field.type(), message));
  }

  private void checkMethod(MethodTree method) {
    if (!isExcluded(method)) {
      for (VariableTree parameter : method.parameters()) {
        getIssueMessage(parameter).ifPresent(message -> reportIssue(parameter.type(), message));
      }
    }
  }

  private static boolean isExcluded(MethodTree methodTree) {
    return methodTree.symbol().isPrivate() || isInConstraintValidator(methodTree);
  }

  private static boolean isInConstraintValidator(MethodTree methodTree) {
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    return enclosingClass != null && JSR380_CONSTRAINT_VALIDATORS.stream().anyMatch(type -> enclosingClass.type().isSubtypeOf(type));
  }

  private static Optional<String> getIssueMessage(VariableTree variable) {
    if (!validationEnabled(variable) && validationSupported(variable)) {
      return Optional.of(MessageFormat.format("Add missing \"@Valid\" on \"{0}\" to validate it with \"Bean Validation\".", variable.simpleName()));
    }
    return Optional.empty();
  }

  private static boolean validationEnabled(VariableTree variable) {
    if (JSR380_VALID_ANNOTATIONS.stream().anyMatch(value -> variable.symbol().metadata().isAnnotatedWith(value))) {
      return true;
    }
    return typeArgumentAnnotations(variable).anyMatch(annotation -> JSR380_VALID_ANNOTATIONS.contains(annotation.fullyQualifiedName()));
  }

  private static Stream<Type> typeArgumentAnnotations(VariableTree variable) {
    return typeArgumentTypeTrees(variable).flatMap(type -> type.annotations().stream()).map(ExpressionTree::symbolType);
  }

  private static Stream<TypeTree> typeArgumentTypeTrees(VariableTree variable) {
    TypeTree variableType = variable.type();
    if (!variableType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return Stream.empty();
    }
    return ((ParameterizedTypeTree) variableType).typeArguments().stream();
  }

  private static boolean validationSupported(VariableTree variable) {
    return annotationInstances(variable).anyMatch(MissingBeanValidationCheck::isConstraintAnnotation);
  }

  private static Stream<SymbolMetadata.AnnotationInstance> annotationInstances(VariableTree variable) {
    if (variable.type().is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return typeArgumentAnnotationInstances(variable);
    }
    Symbol.TypeSymbol classSymbol = variable.symbol().type().symbol();
    return classAndFieldAnnotationInstances(classSymbol);
  }

  private static Stream<SymbolMetadata.AnnotationInstance> typeArgumentAnnotationInstances(VariableTree variable) {
    return typeArgumentTypeTrees(variable).map(TypeTree::symbolType).map(Type::symbol).flatMap(MissingBeanValidationCheck::classAndFieldAnnotationInstances);
  }

  private static Stream<SymbolMetadata.AnnotationInstance> classAndFieldAnnotationInstances(Symbol.TypeSymbol classSymbol) {
    return Stream.concat(classAnnotationInstances(classSymbol), fieldAnnotationInstances(classSymbol));
  }

  private static Stream<SymbolMetadata.AnnotationInstance> classAnnotationInstances(Symbol classSymbol) {
    return classSymbol.metadata().annotations().stream();
  }

  private static Stream<SymbolMetadata.AnnotationInstance> fieldAnnotationInstances(Symbol.TypeSymbol classSymbol) {
    return classSymbol.memberSymbols().stream().flatMap(MissingBeanValidationCheck::classAnnotationInstances);
  }

  private static boolean isConstraintAnnotation(SymbolMetadata.AnnotationInstance annotationInstance) {
    return JSR380_CONSTRAINTS.stream().anyMatch(constraint -> annotationInstance.symbol().metadata().isAnnotatedWith(constraint));
  }
}
