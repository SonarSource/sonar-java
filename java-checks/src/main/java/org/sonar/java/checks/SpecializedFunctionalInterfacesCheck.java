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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
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

  private static final String REPORT_STRING = "Refactor this code to use the more specialised Functional Interface";

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CLASS)) {
      checkClassInterfaces(((ClassTree) tree).superInterfaces());
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      checkVariableTypeAndInitializer((VariableTree) tree);
    }
  }

  public void checkClassInterfaces(ListTree<TypeTree> classInterfaces) {
    List<String> reportMessages = classInterfaces.stream().map(SpecializedFunctionalInterfacesCheck::matchFunctionalInterface).filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
    if (!reportMessages.isEmpty()) {
      if (reportMessages.size() > 1) {
        reportIssue(classInterfaces, REPORT_STRING + "s '" + reportMessages.stream().collect(Collectors.joining(", ")) + "'");
      } else {
        reportIssue(classInterfaces, REPORT_STRING + " '" + reportMessages.get(0) + "'");
      }
    }
  }

  public void checkVariableTypeAndInitializer(VariableTree variableTree) {
    TypeTree interfaceType = variableTree.type();
    ExpressionTree initializer = variableTree.initializer();
    if (initializer != null && (initializer.is(Tree.Kind.LAMBDA_EXPRESSION) || isAnonymousClass(initializer))) {
      matchFunctionalInterface(interfaceType).ifPresent(x -> reportIssue(interfaceType, REPORT_STRING + " '" + x + "'"));
    }
  }

  private static boolean isAnonymousClass(ExpressionTree initializeTree) {
    return initializeTree.is(Tree.Kind.NEW_CLASS) && ((NewClassTree) initializeTree).classBody() != null;
  }

  private static Optional<String> matchFunctionalInterface(TypeTree interfaceType) {
    Optional<TypeArguments> args = interfaceParameters(interfaceType);
    if (args.isPresent()) {
      TypeArguments arguments = args.get();
      switch (interfaceType.symbolType().fullyQualifiedName()) {
        case "java.util.function.Function":
          return handleFunctionInterface(arguments);
        case "java.util.function.BiFunction":
          return handleBiFunctionInterface(arguments);
        case "java.util.function.BiConsumer":
          return handleBiConsumerInterface(arguments);
        case "java.util.function.Supplier":
        case "java.util.function.Consumer":
        case "java.util.function.Predicate":
        case "java.util.function.UnaryOperator":
        case "java.util.function.BinaryOperator":
          return handleOneParameterInterface(arguments).map(s -> s + interfaceType.symbolType().name());
        default:
          return Optional.empty();
      }
    }
    return Optional.empty();
  }

  private static Optional<String> handleOneParameterInterface(TypeArguments args) {
    String reportString = new ParameterTypeNameAndTreeType(args.get(0)).paramTypeName;
    if (reportString != null) {
      return Optional.of(reportString);
    }
    return Optional.empty();
  }

  private static Optional<String> handleFunctionInterface(TypeArguments args) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(args.get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(args.get(1));
    String reportString = null;
    if (firstArgument.paramType.equals(secondArgument.paramType)) {
      reportString = "UnaryOperator<" + firstArgument.paramType.name() + ">";
    } else if (firstArgument.paramTypeName != null && secondArgument.paramTypeName != null) {
      reportString = firstArgument.paramTypeName + "To" + secondArgument.paramTypeName + "Function";
    } else if (firstArgument.paramTypeName == null && secondArgument.paramTypeName != null) {
      reportString = "To" + secondArgument.paramTypeName + "Function<" + firstArgument.paramType.name() + ">";
    } else if (firstArgument.paramTypeName != null) {
      reportString = firstArgument.paramTypeName + "Function<" + secondArgument.paramType.name() + ">";
    }
    if (reportString != null) {
      return Optional.of(reportString);
    }
    return Optional.empty();
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
    String reportString = returnStringFromJavaObject(((IdentifierTree) args.get(1)).symbolType());
    if (reportString != null) {
      return Optional.of("Obj" + reportString + "Consumer");
    }
    return Optional.empty();
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

  private static Optional<TypeArguments> interfaceParameters(TypeTree interfaceType) {
    if (interfaceType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return Optional.of(((ParameterizedTypeTree) interfaceType).typeArguments());
    }
    return Optional.empty();
  }

  private static class ParameterTypeNameAndTreeType {
    @Nullable
    Type paramType;

    @Nullable
    String paramTypeName;

    public ParameterTypeNameAndTreeType(Tree tree) {
      switch (tree.kind()) {
        case IDENTIFIER:
          IdentifierTree idTree = (IdentifierTree) tree;
          this.paramType = idTree.symbolType();
          this.paramTypeName = returnStringFromJavaObject(this.paramType);
          break;
        case MEMBER_SELECT:
          MemberSelectExpressionTree memberTree = (MemberSelectExpressionTree) tree;
          this.paramType = memberTree.symbolType();
          this.paramTypeName = returnStringFromJavaObject(this.paramType);
          break;
        case PARAMETERIZED_TYPE:
          ParameterizedTypeTree p = (ParameterizedTypeTree) tree;
          this.paramTypeName = null;
          this.paramType = p.symbolType();
          break;
        case EXTENDS_WILDCARD:
        case UNBOUNDED_WILDCARD:
        case SUPER_WILDCARD:
          WildcardTree w = (WildcardTree) tree;
          this.paramTypeName = null;
          this.paramType = w.symbolType();
          break;
        default:
          break;
      }
    }

  }
}
