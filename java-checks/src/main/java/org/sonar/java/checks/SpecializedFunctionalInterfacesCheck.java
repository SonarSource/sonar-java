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
import java.util.Collections;
import java.util.List;
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
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

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
    List<InterfaceTreeAndStringPairReport> reportTreeAndStringInterfaces = tree.superInterfaces().stream().map(SpecializedFunctionalInterfacesCheck::matchFunctionalInterface)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
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
      matchFunctionalInterface(variableTree.type())
        .ifPresent(treeAndStringInterface -> reportIssue(treeAndStringInterface.classInterface, reportMessage(treeAndStringInterface)));
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

  private static Optional<InterfaceTreeAndStringPairReport> matchFunctionalInterface(TypeTree interfaceType) {
    Optional<TypeArguments> args = interfaceParameters(interfaceType);
    if (args.isPresent()) {
      TypeArguments arguments = args.get();
      Optional<String> reportString;
      switch (interfaceType.symbolType().fullyQualifiedName()) {
        case "java.util.function.Function":
          reportString = handleFunctionInterface(arguments);
          break;
        case "java.util.function.BiFunction":
          reportString = handleBiFunctionInterface(arguments);
          break;
        case "java.util.function.BiConsumer":
          reportString = handleBiConsumerInterface(arguments);
          break;
        case "java.util.function.Supplier":
        case "java.util.function.Consumer":
        case "java.util.function.Predicate":
        case "java.util.function.UnaryOperator":
        case "java.util.function.BinaryOperator":
          reportString = handleOneParameterInterface(arguments).map(s -> s + interfaceType.symbolType().name());
          break;
        default:
          return Optional.empty();
      }
      return reportString.map(rs -> new InterfaceTreeAndStringPairReport(rs, interfaceType));
    }
    return Optional.empty();
  }

  private static Optional<String> handleOneParameterInterface(TypeArguments args) {
    return Optional.ofNullable(new ParameterTypeNameAndTreeType(args.get(0)).paramTypeName);
  }

  private static Optional<String> handleFunctionInterface(TypeArguments args) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(args.get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(args.get(1));
    String reportString = null;
    if (firstArgument.paramType.equals(secondArgument.paramType)) {
      reportString = String.format("UnaryOperator<%s>", firstArgument.paramType.name());
    } else if (firstArgument.paramTypeName != null && secondArgument.paramTypeName != null) {
      reportString = String.format("%sTo%sFunction", firstArgument.paramTypeName, secondArgument.paramTypeName);
    } else if (firstArgument.paramTypeName == null && secondArgument.paramTypeName != null) {
      reportString = String.format("To%sFunction<%s>", secondArgument.paramTypeName, firstArgument.paramType.name());
    } else if (firstArgument.paramTypeName != null) {
      reportString = String.format("%sFunction<%s>", firstArgument.paramTypeName, secondArgument.paramType.name());
    }
    return Optional.ofNullable(reportString);
  }

  private static Optional<String> handleBiFunctionInterface(TypeArguments args) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(args.get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(args.get(1));
    ParameterTypeNameAndTreeType thirdArgument = new ParameterTypeNameAndTreeType(args.get(2));
    if (firstArgument.paramType.equals(secondArgument.paramType) && firstArgument.paramType.equals(thirdArgument.paramType)) {
      return Optional.of("BinaryOperator<" + firstArgument.paramType.name() + ">");
    }
    return Optional.empty();
  }

  private static Optional<String> handleBiConsumerInterface(TypeArguments args) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(args.get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(args.get(1));
    if (secondArgument.paramTypeName != null) {
      return Optional.ofNullable(String.format("Obj%sConsumer<%s>", secondArgument.paramTypeName, firstArgument.paramType));
    }
    return Optional.empty();
  }

  private static Optional<TypeArguments> interfaceParameters(TypeTree interfaceType) {
    if (interfaceType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return Optional.of(((ParameterizedTypeTree) interfaceType).typeArguments());
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

    Type paramType;

    @Nullable
    String paramTypeName;

    public ParameterTypeNameAndTreeType(Tree tree) {
      switch (tree.kind()) {
        case IDENTIFIER:
          paramType = ((IdentifierTree) tree).symbolType();
          paramTypeName = returnStringFromJavaObject(paramType);
          break;
        case MEMBER_SELECT:
          paramType = ((MemberSelectExpressionTree) tree).symbolType();
          paramTypeName = returnStringFromJavaObject(paramType);
          break;
        case PARAMETERIZED_TYPE:
          paramType = ((ParameterizedTypeTree) tree).symbolType();
          paramTypeName = null;
          break;
        case EXTENDS_WILDCARD:
        case UNBOUNDED_WILDCARD:
        case SUPER_WILDCARD:
          paramType = ((WildcardTree) tree).symbolType();
          paramTypeName = null;
          break;
        default:
          break;
      }
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
