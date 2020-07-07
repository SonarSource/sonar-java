/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4276")
public class SpecializedFunctionalInterfacesCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CLASS)) {
      checkClassInterfaces(((ClassTree) tree));
    } else {
      checkVariableTypeAndInitializer((VariableTree) tree);
    }
  }

  private void checkClassInterfaces(ClassTree tree) {
    List<InterfaceTreeAndStringPairReport> reportTreeAndStringInterfaces = tree.superInterfaces().stream()
      .map(typeTree -> matchFunctionalInterface(typeTree.symbolType(), false)
      .map(rs -> new InterfaceTreeAndStringPairReport(rs, typeTree)).orElse(null))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    if (reportTreeAndStringInterfaces.isEmpty()) {
      return;
    }
    List<JavaFileScannerContext.Location> secondaryLocations = reportTreeAndStringInterfaces.stream()
      .map(interf -> new JavaFileScannerContext.Location("", interf.classInterface))
      .collect(Collectors.toList());
    reportIssue(tree.simpleName(), reportMessage(reportTreeAndStringInterfaces), secondaryLocations, null);
  }

  private void checkVariableTypeAndInitializer(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    if ((variableTree.symbol().owner().isMethodSymbol() && !variableTree.parent().is(Tree.Kind.LAMBDA_EXPRESSION))
      || (initializer != null && (initializer.is(Tree.Kind.LAMBDA_EXPRESSION) || isAnonymousClass(initializer)))) {
      boolean usedAsMethodReference = isReferenced(variableTree.symbol().usages());
      matchFunctionalInterface(variableTree.symbol().type(), usedAsMethodReference).ifPresent(reportString -> {
        TypeTree variableType = variableTree.type();
        reportIssue(variableType, reportMessage(new InterfaceTreeAndStringPairReport(reportString, variableType)));
      });
    }
  }

  private static String reportMessage(InterfaceTreeAndStringPairReport onlyOneInterface) {
    return reportMessage(Collections.singletonList(onlyOneInterface));
  }

  private static String reportMessage(List<InterfaceTreeAndStringPairReport> interfacesToBeReported) {
    String functionalInterfaces = interfacesToBeReported.stream().map(x -> x.reportString)
      .collect(Collectors.joining("', '", (interfacesToBeReported.size() > 1 ? "s '" : " '"), "'"));
    return String.format("Refactor this code to use the more specialised Functional Interface%s", functionalInterfaces);
  }

  private static boolean isAnonymousClass(ExpressionTree initializeTree) {
    return initializeTree.is(Tree.Kind.NEW_CLASS) && ((NewClassTree) initializeTree).classBody() != null;
  }

  private static Optional<String> matchFunctionalInterface(Type type, boolean usedAsMethodReference) {
    if (type.isUnknown() || !type.isParameterized()) {
      return Optional.empty();
    }
    switch (type.fullyQualifiedName()) {
      case "java.util.function.Function":
        return handleFunctionInterface(type, usedAsMethodReference);
      case "java.util.function.BiFunction":
        return handleBiFunctionInterface(type);
      case "java.util.function.BiConsumer":
        return handleBiConsumerInterface(type, usedAsMethodReference);
      case "java.util.function.Supplier":
        return handleSupplier(type, usedAsMethodReference);
      case "java.util.function.Consumer":
      case "java.util.function.Predicate":
      case "java.util.function.UnaryOperator":
      case "java.util.function.BinaryOperator":
        return handleSingleParameterFunctions(type, usedAsMethodReference);
      default:
        return Optional.empty();
    }
  }

  private static Optional<String> handleSingleParameterFunctions(Type parametrizedType, boolean usedAsMethodReference) {
    if (usedAsMethodReference) {
      return Optional.empty();
    }
    return Optional.ofNullable(new ParameterTypeNameAndTreeType(parametrizedType, 0).paramTypeName)
      .map(s -> s + parametrizedType.name());
  }

  private static Optional<String> handleFunctionInterface(Type parametrizedType, boolean usedAsMethodReference) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(parametrizedType, 0);
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(parametrizedType, 1);
    if (typeEquals(firstArgument.paramType, secondArgument.paramType)) {
      if (firstArgument.paramTypeName != null && !usedAsMethodReference) {
        return functionalInterfaceName("%sUnaryOperator", firstArgument.paramTypeName);
      }
      return functionalInterfaceName("UnaryOperator<%s>", firstArgument.paramType);
    }
    if (isBoolean(secondArgument) && !usedAsMethodReference) {
      return functionalInterfaceName("Predicate<%s>", firstArgument.paramType);
    }
    if (isBoolean(firstArgument)) {
      return Optional.empty();
    }
    if (firstArgument.paramTypeName != null && secondArgument.paramTypeName != null && !usedAsMethodReference) {
      return functionalInterfaceName("%sTo%sFunction", firstArgument.paramTypeName, secondArgument.paramTypeName);
    }
    if (secondArgument.paramTypeName != null && !usedAsMethodReference) {
      return functionalInterfaceName("To%sFunction<%s>", secondArgument.paramTypeName, firstArgument.paramType);
    }
    if (firstArgument.paramTypeName != null && !usedAsMethodReference) {
      return functionalInterfaceName("%sFunction<%s>", firstArgument.paramTypeName, secondArgument.paramType);
    }
    return Optional.empty();
  }

  private static Optional<String> handleBiFunctionInterface(Type parametrizedType) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(parametrizedType, 0);
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(parametrizedType, 1);
    ParameterTypeNameAndTreeType thirdArgument = new ParameterTypeNameAndTreeType(parametrizedType, 2);
    if (typeEquals(firstArgument.paramType, secondArgument.paramType) && typeEquals(firstArgument.paramType, thirdArgument.paramType)) {
      return functionalInterfaceName("BinaryOperator<%s>", firstArgument.paramType);
    }
    if (isBoolean(thirdArgument)) {
      return functionalInterfaceName("BiPredicate<%s, %s>", firstArgument.paramType, secondArgument.paramType);
    }
    return Optional.empty();
  }

  private static Optional<String> functionalInterfaceName(String pattern, Object... args) {
    return Optional.of(String.format(pattern, args));
  }

  private static Optional<String> handleBiConsumerInterface(Type parametrizedType, boolean usedAsMethodReference) {
    if (usedAsMethodReference) {
      return Optional.empty();
    }
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(parametrizedType, 0);
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(parametrizedType, 1);
    if (secondArgument.paramTypeName != null) {
      return Optional.of(String.format("Obj%sConsumer<%s>", secondArgument.paramTypeName, firstArgument.paramType));
    }
    return Optional.empty();
  }

  private static Optional<String> handleSupplier(Type parametrizedType, boolean usedAsMethodReference) {
    if (usedAsMethodReference) {
      return Optional.empty();
    }
    ParameterTypeNameAndTreeType supplierParamType = new ParameterTypeNameAndTreeType(parametrizedType, 0);
    if (isBoolean(supplierParamType)) {
      return Optional.of("BooleanSupplier");
    }
    return Optional.ofNullable(supplierParamType.paramTypeName).map(s -> s + "Supplier");
  }

  private static class InterfaceTreeAndStringPairReport {
    final String reportString;
    final TypeTree classInterface;

    InterfaceTreeAndStringPairReport(String report, TypeTree interf) {
      reportString = report;
      classInterface = interf;
    }
  }

  private static boolean isBoolean(ParameterTypeNameAndTreeType type) {
    return type.paramType.is("java.lang.Boolean");
  }

  private static class ParameterTypeNameAndTreeType {

    final Type paramType;

    @Nullable
    final String paramTypeName;

    ParameterTypeNameAndTreeType(Type parametrizedType, int typeArgumentIndex) {
      paramType = parametrizedType.typeArguments().get(typeArgumentIndex);
      paramTypeName = returnStringFromJavaObject(paramType);
    }

    @CheckForNull
    private static String returnStringFromJavaObject(Type argType) {
      if (argType.is("java.lang.Integer")) {
        return "Int";
      }
      if (argType.is("java.lang.Double") || argType.is("java.lang.Long")) {
        return argType.name();
      }
      return null;
    }
  }

  private static boolean isReferenced(List<IdentifierTree> usages) {
    return usages.stream()
      .map(Tree::parent)
      .anyMatch(parent -> parent.is(Tree.Kind.ARGUMENTS, Tree.Kind.ASSIGNMENT, Tree.Kind.VARIABLE));
  }

  private static boolean typeEquals(Type type1, Type type2) {
    return !type1.name().startsWith("?") && type1.equals(type2);
  }

}
