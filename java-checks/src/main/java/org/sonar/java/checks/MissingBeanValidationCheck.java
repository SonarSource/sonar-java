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

import com.google.common.collect.ImmutableList;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SymbolUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
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
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CLASS)) {
      checkFields((ClassTree) tree);
    } else if (tree.is(Tree.Kind.METHOD)) {
      checkMethod((MethodTree) tree);
    }
  }

  private void checkFields(ClassTree tree) {
    JavaSymbol.TypeJavaSymbol typeJavaSymbol = (JavaSymbol.TypeJavaSymbol) tree.symbol();
    typeJavaSymbol.memberSymbols().stream().filter(SymbolUtils::isField).map(Symbol::declaration).map(VariableTree.class::cast)
      .forEach(declaration -> getIssueMessage(declaration).ifPresent(message -> reportIssue(declaration.type(), message)));
  }

  private void checkMethod(MethodTree method) {
    for (VariableTree parameter : method.parameters()) {
      getIssueMessage(parameter).ifPresent(s -> reportIssue(parameter.type(), s));
    }
  }

  private Optional<String> getIssueMessage(VariableTree variable) {
    if (!validationEnabled(variable) && validationSupported(variable)) {
      return Optional.of(MessageFormat.format("Add missing \"@Valid\" on \"{0}\" to validate it with \"Bean Validation\".", variable.simpleName()));
    }
    return Optional.empty();
  }

  private boolean validationEnabled(VariableTree variable) {
    Type type = variable.symbol().type();
    boolean result = variable.symbol().metadata().isAnnotatedWith(JAVAX_VALIDATION_VALID);
    if (type instanceof ParametrizedTypeJavaType) {
      result = result || typeArgumentAnnotationNames(variable).anyMatch(name -> name.equals(JAVAX_VALIDATION_VALID));
    }
    return result;
  }

  private Stream<String> typeArgumentAnnotationNames(VariableTree variable) {
    return typeArgumentTypeTrees(variable).flatMap(type -> type.annotations().stream()).map(annotation -> annotation.symbolType().fullyQualifiedName());
  }

  private Stream<TypeTree> typeArgumentTypeTrees(VariableTree variable) {
    ParameterizedTypeTree parameterizedType = (ParameterizedTypeTree) variable.type();
    return parameterizedType.typeArguments().stream().map(TypeTree.class::cast);
  }

  private boolean validationSupported(VariableTree variable) {
    return annotationInstances(variable).anyMatch(this::isConstraintAnnotation);
  }

  private Stream<SymbolMetadata.AnnotationInstance> annotationInstances(VariableTree variable) {
    if (variable.type().is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return typeArgumentAnnotationInstances(variable);
    } else {
      JavaSymbol.TypeJavaSymbol classSymbol = ((JavaType) variable.symbol().type()).getSymbol();
      return classAndFieldAnnotationInstances(classSymbol);
    }
  }

  private Stream<SymbolMetadata.AnnotationInstance> typeArgumentAnnotationInstances(VariableTree variable) {
    return typeArgumentTypeTrees(variable).map(this::typeSymbol).flatMap(this::classAndFieldAnnotationInstances);
  }

  private JavaSymbol.TypeJavaSymbol typeSymbol(TypeTree type) {
    return ((JavaType) type.symbolType()).getSymbol();
  }

  private Stream<SymbolMetadata.AnnotationInstance> classAndFieldAnnotationInstances(JavaSymbol.TypeJavaSymbol classSymbol) {
    return Stream.concat(classAnnotationInstances(classSymbol), fieldAnnotationInstances(classSymbol));
  }

  private Stream<SymbolMetadata.AnnotationInstance> classAnnotationInstances(JavaSymbol classSymbol) {
    return classSymbol.metadata().annotations().stream();
  }

  private Stream<SymbolMetadata.AnnotationInstance> fieldAnnotationInstances(JavaSymbol.TypeJavaSymbol classSymbol) {
    return classSymbol.memberSymbols().stream().map(JavaSymbol.class::cast).flatMap(this::classAnnotationInstances);
  }

  private boolean isConstraintAnnotation(SymbolMetadata.AnnotationInstance annotationInstance) {
    return ((JavaSymbol.TypeJavaSymbol) annotationInstance.symbol()).metadata().isAnnotatedWith(JAVAX_VALIDATION_CONSTRAINT);
  }
}
