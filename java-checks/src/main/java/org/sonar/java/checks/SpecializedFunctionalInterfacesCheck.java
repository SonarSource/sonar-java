/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.java.resolve.TypeVariableJavaType;
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
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CLASS)) {
      checkClassInterfaces(((ClassTree) tree));
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      checkVariableTypeAndInitializer((VariableTree) tree);
    }
  }

  public void checkClassInterfaces(ClassTree tree) {
    List<InterfaceTreeAndStringPairReport> reportTreeAndStringInterfaces = new ArrayList<>();
    List<TypeTree> interfacesList = tree.superInterfaces();
    if (!interfacesList.isEmpty()) {
      IntStream.range(0, interfacesList.size())
        .forEach(i -> {
          JavaType jt = (JavaType) tree.symbol().interfaces().get(i);
          if (jt.isParameterized()) {
            Optional<String> reportString = matchFunctionalInterface(jt);
            reportString.ifPresent(rs -> reportTreeAndStringInterfaces.add(new InterfaceTreeAndStringPairReport(rs, interfacesList.get(i))));
          }
        });
    }
    if (!reportTreeAndStringInterfaces.isEmpty()) {
      List<JavaFileScannerContext.Location> flow = reportTreeAndStringInterfaces.stream()
        .map(interf -> new JavaFileScannerContext.Location("", interf.classInterface))
        .collect(Collectors.toList());
      reportIssue(tree.simpleName(), reportMessage(reportTreeAndStringInterfaces), flow, null);

    }
  }

  public void checkVariableTypeAndInitializer(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    if (initializer != null && (initializer.is(Tree.Kind.LAMBDA_EXPRESSION) || isAnonymousClass(initializer))) {
      matchFunctionalInterface((JavaType) variableTree.symbol().type());
      JavaType jt = ((JavaType) variableTree.symbol().type());
      if (jt.isParameterized()) {
        matchFunctionalInterface(jt)
          .ifPresent(reportString -> reportIssue(variableTree.type(), reportMessage(new InterfaceTreeAndStringPairReport(reportString, variableTree.type()))));
      }
    }
  }

  private static String reportMessage(InterfaceTreeAndStringPairReport onlyOneInterface) {
    return reportMessage(Collections.singletonList(onlyOneInterface));
  }

  private static String reportMessage(List<InterfaceTreeAndStringPairReport> interfacesToBeReported) {
    String functionalInterfaces = interfacesToBeReported.stream().map(x -> x.reportString).collect(Collectors.joining(", "));
    String manyInterfaces = (interfacesToBeReported.size() > 1 ? "s" : "") + " '";
    return String.format("Refactor this code to use the more specialised Functional Interface%s%s'", manyInterfaces, functionalInterfaces);
  }

  private static boolean isAnonymousClass(ExpressionTree initializeTree) {
    return initializeTree.is(Tree.Kind.NEW_CLASS) && ((NewClassTree) initializeTree).classBody() != null;
  }

  private static Optional<String> matchFunctionalInterface(JavaType jt) {
    Optional<String> reportString;
    switch (jt.getSymbol().getFullyQualifiedName()) {
      case "java.util.function.Function":
        reportString = handleFunctionInterface((ParametrizedTypeJavaType) jt);
        break;
      case "java.util.function.BiFunction":
        reportString = handleBiFunctionInterface((ParametrizedTypeJavaType) jt);
        break;
      case "java.util.function.BiConsumer":
        reportString = handleBiConsumerInterface((ParametrizedTypeJavaType) jt);
        break;
      case "java.util.function.Supplier":
      case "java.util.function.Consumer":
      case "java.util.function.Predicate":
      case "java.util.function.UnaryOperator":
      case "java.util.function.BinaryOperator":
        reportString = handleOneParameterInterface((ParametrizedTypeJavaType) jt).map(s -> s + jt.name());
        break;
      default:
        return Optional.empty();
    }
    return reportString;
  }

  private static Optional<String> handleOneParameterInterface(ParametrizedTypeJavaType ptjt) {
    return Optional.ofNullable(new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(0)).paramTypeName);
  }

  private static Optional<String> handleFunctionInterface(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(1));
    String reportString = null;
    if (firstArgument.paramType.equals(secondArgument.paramType)) {
      reportString = String.format("UnaryOperator<%s>", firstArgument.paramType);
    } else if (firstArgument.paramTypeName != null && secondArgument.paramTypeName != null) {
      reportString = String.format("%sTo%sFunction", firstArgument.paramTypeName, secondArgument.paramTypeName);
    } else if (firstArgument.paramTypeName == null && secondArgument.paramTypeName != null) {
      reportString = String.format("To%sFunction<%s>", secondArgument.paramTypeName, firstArgument.paramType);
    } else if (firstArgument.paramTypeName != null) {
      reportString = String.format("%sFunction<%s>", firstArgument.paramTypeName, secondArgument.paramType);
    }
    return Optional.ofNullable(reportString);
  }

  private static Optional<String> handleBiFunctionInterface(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(1));
    ParameterTypeNameAndTreeType thirdArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(2));
    if (firstArgument.paramType.equals(secondArgument.paramType) && firstArgument.paramType.equals(thirdArgument.paramType)) {
      return Optional.of("BinaryOperator<" + firstArgument.paramType + ">");
    }
    return Optional.empty();
  }

  private static Optional<String> handleBiConsumerInterface(ParametrizedTypeJavaType ptjt) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(ptjt, ptjt.typeParameters().get(1));
    if (secondArgument.paramTypeName != null) {
      return Optional.ofNullable(String.format("Obj%sConsumer<%s>", secondArgument.paramTypeName, firstArgument.paramType));
    }
    return Optional.empty();
  }

  private static class InterfaceTreeAndStringPairReport {
    final String reportString;
    final TypeTree classInterface;

    public InterfaceTreeAndStringPairReport(String report, TypeTree interf) {
      reportString = report;
      classInterface = interf;
    }
  }

  private static class ParameterTypeNameAndTreeType {

    final JavaType paramType;

    @Nullable
    final String paramTypeName;

    public ParameterTypeNameAndTreeType(ParametrizedTypeJavaType ptjt, TypeVariableJavaType tvjt) {
      paramType = ptjt.substitution(tvjt);
      paramTypeName = returnStringFromJavaObject(paramType);
    }

    @CheckForNull
    private static String returnStringFromJavaObject(Type argType) {
      if (argType.is("java.lang.Integer")) {
        return "Int";
      } else if (argType.is("java.lang.Boolean") || argType.is("java.lang.Double") || argType.is("java.lang.Long")) {
        return argType.name();
      } else {
        return null;
      }
    }
  }
}
