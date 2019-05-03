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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
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

  public void checkClassInterfaces(ClassTree tree) {
    List<InterfaceTreeAndStringPairReport> reportTreeAndStringInterfaces = tree.superInterfaces().stream()
      .map(typeTree -> matchFunctionalInterface(typeTree.symbolType()).map(rs -> new InterfaceTreeAndStringPairReport(rs, typeTree)).orElse(null))
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

  public void checkVariableTypeAndInitializer(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    if ((variableTree.symbol().owner().isMethodSymbol() && !variableTree.parent().is(Tree.Kind.LAMBDA_EXPRESSION))
      || (initializer != null && (initializer.is(Tree.Kind.LAMBDA_EXPRESSION) || isAnonymousClass(initializer)))) {
      matchFunctionalInterface((variableTree.symbol().type())).ifPresent(reportString -> {
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

  private static Optional<String> matchFunctionalInterface(Type type) {
    JavaType javaType = (JavaType) type;
    if (!javaType.isParameterized()) {
      return Optional.empty();
    }
    ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) javaType;

    if (hasAnyUnknownParameterType(ptjt)) {
      return Optional.empty();
    }

    switch (ptjt.getSymbol().getFullyQualifiedName()) {
      case "java.util.function.Function":
        return handleFunctionInterface(ptjt);
      case "java.util.function.BiFunction":
        return handleBiFunctionInterface(ptjt);
      case "java.util.function.BiConsumer":
        return handleBiConsumerInterface(ptjt);
      case "java.util.function.Supplier":
        return handleSupplier(ptjt);
      case "java.util.function.Consumer":
      case "java.util.function.Predicate":
      case "java.util.function.UnaryOperator":
      case "java.util.function.BinaryOperator":
        return handleSingleParameterFunctions(ptjt);
      default:
        return Optional.empty();
    }
  }

  private static boolean hasAnyUnknownParameterType(ParametrizedTypeJavaType ptjt) {
    return ptjt.typeParameters().stream().map(ptjt::substitution).anyMatch(Type::isUnknown);
  }

  private static Optional<String> handleSingleParameterFunctions(ParametrizedTypeJavaType ptjt) {
    return Optional.ofNullable(new ParameterTypeNameAndTreeType(ptjt, 0).paramTypeName).map(s -> s + ptjt.name());
  }

  private static Optional<String> handleFunctionInterface(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(ptjt, 0);
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(ptjt, 1);
    if (firstArgument.paramType.equals(secondArgument.paramType)) {
      if (firstArgument.paramTypeName != null) {
        return functionalInterfaceName("%sUnaryOperator", firstArgument.paramTypeName);
      }
      return functionalInterfaceName("UnaryOperator<%s>", firstArgument.paramType);
    }
    if (isBoolean(secondArgument)) {
      return functionalInterfaceName("Predicate<%s>", firstArgument.paramType);
    }
    if (isBoolean(firstArgument)) {
      return Optional.empty();
    }
    if (firstArgument.paramTypeName != null && secondArgument.paramTypeName != null) {
      return functionalInterfaceName("%sTo%sFunction", firstArgument.paramTypeName, secondArgument.paramTypeName);
    }
    if (firstArgument.paramTypeName == null && secondArgument.paramTypeName != null) {
      return functionalInterfaceName("To%sFunction<%s>", secondArgument.paramTypeName, firstArgument.paramType);
    }
    if (firstArgument.paramTypeName != null) {
      return functionalInterfaceName("%sFunction<%s>", firstArgument.paramTypeName, secondArgument.paramType);
    }
    return Optional.empty();
  }

  private static Optional<String> handleBiFunctionInterface(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(ptjt, 0);
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(ptjt, 1);
    ParameterTypeNameAndTreeType thirdArgument = new ParameterTypeNameAndTreeType(ptjt, 2);
    if (firstArgument.paramType.equals(secondArgument.paramType) && firstArgument.paramType.equals(thirdArgument.paramType)) {
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

  private static Optional<String> handleBiConsumerInterface(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(ptjt, 0);
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(ptjt, 1);
    if (secondArgument.paramTypeName != null) {
      return Optional.of(String.format("Obj%sConsumer<%s>", secondArgument.paramTypeName, firstArgument.paramType));
    }
    return Optional.empty();
  }

  private static Optional<String> handleSupplier(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType supplierParamType = new ParameterTypeNameAndTreeType(ptjt, 0);
    if (isBoolean(supplierParamType)) {
      return Optional.of("BooleanSupplier");
    }
    return Optional.ofNullable(supplierParamType.paramTypeName).map(s -> s + "Supplier");
  }

  private static class InterfaceTreeAndStringPairReport {
    final String reportString;
    final TypeTree classInterface;

    public InterfaceTreeAndStringPairReport(String report, TypeTree interf) {
      reportString = report;
      classInterface = interf;
    }
  }

  private static boolean isBoolean(ParameterTypeNameAndTreeType type) {
    return type.paramType.is("java.lang.Boolean");
  }

  private static class ParameterTypeNameAndTreeType {

    final JavaType paramType;

    @Nullable
    final String paramTypeName;

    public ParameterTypeNameAndTreeType(ParametrizedTypeJavaType ptjt, int typeVarIndex) {
      paramType = ptjt.substitution(ptjt.typeParameters().get(typeVarIndex));
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
}
