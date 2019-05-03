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
  private static final String JAVAX_VALIDATION_VALID = "javax.validation.Valid";
  private static final String JAVAX_VALIDATION_CONSTRAINT = "javax.validation.Constraint";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

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
    for (VariableTree parameter : method.parameters()) {
      getIssueMessage(parameter).ifPresent(message -> reportIssue(parameter.type(), message));
    }
  }

  private static Optional<String> getIssueMessage(VariableTree variable) {
    if (!validationEnabled(variable) && validationSupported(variable)) {
      return Optional.of(MessageFormat.format("Add missing \"@Valid\" on \"{0}\" to validate it with \"Bean Validation\".", variable.simpleName()));
    }
    return Optional.empty();
  }

  private static boolean validationEnabled(VariableTree variable) {
    if (variable.symbol().metadata().isAnnotatedWith(JAVAX_VALIDATION_VALID)) {
      return true;
    }
    return typeArgumentAnnotations(variable).anyMatch(annotation -> annotation.is(JAVAX_VALIDATION_VALID));
  }

  private static Stream<Type> typeArgumentAnnotations(VariableTree variable) {
    return typeArgumentTypeTrees(variable).flatMap(type -> type.annotations().stream()).map(ExpressionTree::symbolType);
  }

  private static Stream<TypeTree> typeArgumentTypeTrees(VariableTree variable) {
    TypeTree variableType = variable.type();
    if (!variableType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return Stream.empty();
    }
    return ((ParameterizedTypeTree) variableType).typeArguments().stream().map(TypeTree.class::cast);
  }

  private static boolean validationSupported(VariableTree variable) {
    return annotationInstances(variable).anyMatch(MissingBeanValidationCheck::isConstraintAnnotation);
  }

  private static Stream<SymbolMetadata.AnnotationInstance> annotationInstances(VariableTree variable) {
    if (variable.type().is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return typeArgumentAnnotationInstances(variable);
    } else {
      Symbol.TypeSymbol classSymbol = variable.symbol().type().symbol();
      return classAndFieldAnnotationInstances(classSymbol);
    }
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
    return annotationInstance.symbol().metadata().isAnnotatedWith(JAVAX_VALIDATION_CONSTRAINT);
  }
}
