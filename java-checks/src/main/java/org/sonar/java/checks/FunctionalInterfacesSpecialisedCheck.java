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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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
public class FunctionalInterfacesSpecialisedCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.VARIABLE);
  }

  private static final String REPORT_STRING = "Refactor this code to use the more specialised Functional Interface '";
  private static final String UNARY_OPERATOR = "UnaryOperator<";

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CLASS)) {
      ClassTree interfaceTree = (ClassTree) tree;
      List<String> reportMessages = interfaceTree.superInterfaces().stream().map(FunctionalInterfacesSpecialisedCheck::matchInterface).filter(Objects::nonNull)
        .collect(Collectors.toList());
      if (!reportMessages.isEmpty()) {
        report(interfaceTree.superInterfaces(), reportMessages);
      }
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) tree;
      TypeTree interfaceType = variableTree.type();
      ExpressionTree initializer = variableTree.initializer();
      String reportMessage = null;
      if (initializer != null && (initializer.is(Tree.Kind.LAMBDA_EXPRESSION) ||
        (initializer.is(Tree.Kind.NEW_CLASS) && ((NewClassTree) initializer).classBody() != null))) {
        reportMessage = matchInterface(interfaceType);
        report(interfaceType, reportMessage);
      }
    }
  }

  @CheckForNull
  private static String matchInterface(TypeTree interfaceType) {
    TypeArguments args = interfaceParameters(interfaceType);
    if (args == null) {
      return null;
    }
    String reportString = null;
    switch (interfaceType.symbolType().fullyQualifiedName()) {
      case "java.util.function.Function":
        return handleFunctionInterface(args);
      case "java.util.function.BiFunction":
        return handleBiFunctionInterface(args);
      case "java.util.function.Supplier":
        reportString = handleOneParameterInterface(args);
        return reportString != null ? (reportString + "Supplier") : null;
      case "java.util.function.Consumer":
        reportString = handleOneParameterInterface(args);
        return reportString != null ? (reportString + "Consumer") : null;
      case "java.util.function.BiConsumer":
        return handleBiConsumerInterface(args);
      case "java.util.function.Predicate":
        reportString = handleOneParameterInterface(args);
        return reportString != null ? (reportString + "Predicate") : null;
      case "java.util.function.UnaryOperator":
        reportString = handleOneParameterInterface(args);
        return reportString != null ? (reportString + "UnaryOperator") : null;
      case "java.util.function.BinaryOperator":
        reportString = handleOneParameterInterface(args);
        return reportString != null ? (reportString + "BinaryOperator") : null;
      default:
        return null;
    }
  }

  @CheckForNull
  private static String handleOneParameterInterface(TypeArguments args) {
    return new ParameterTypeNameAndTreeType(args.get(0)).paramTypeName;
  }

  @CheckForNull
  private static String handleFunctionInterface(TypeArguments args) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(args.get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(args.get(1));

    if (firstArgument.paramTypeName != null && secondArgument.paramTypeName != null) {
      if (firstArgument.paramTypeName.equals(secondArgument.paramTypeName)) {
        return (UNARY_OPERATOR + firstArgument.originalName + ">");
      }
      return firstArgument.paramTypeName + "To" + secondArgument.paramTypeName + "Function";
    } else if (firstArgument.paramTypeName == null && secondArgument.paramTypeName != null) {
      return "To" + secondArgument.paramTypeName + "Function<" + firstArgument.originalName + ">";
    } else if (firstArgument.paramTypeName != null) {
      return firstArgument.paramTypeName + "Function<" + secondArgument.originalName + ">";
    } else if (firstArgument.originalName.equals(secondArgument.originalName)) {
      return !args.get(0).is(Tree.Kind.PARAMETERIZED_TYPE) ? (UNARY_OPERATOR + firstArgument.originalName + ">") : (UNARY_OPERATOR + firstArgument.originalName);
    }
    return null;
  }

  @CheckForNull
  private static String handleBiFunctionInterface(TypeArguments args) {
    ParameterTypeNameAndTreeType firstArgument = new ParameterTypeNameAndTreeType(args.get(0));
    ParameterTypeNameAndTreeType secondArgument = new ParameterTypeNameAndTreeType(args.get(1));
    ParameterTypeNameAndTreeType thirdArgument = new ParameterTypeNameAndTreeType(args.get(2));
    return (firstArgument.originalName.equals(secondArgument.originalName) &&
      firstArgument.originalName.equals(thirdArgument.originalName)) ? ("BinaryOperator<" + firstArgument.originalName + ">") : null;
  }

  @CheckForNull
  private static String handleBiConsumerInterface(TypeArguments args) {
    return "Obj" + returnStringFromJavaObject(((IdentifierTree) args.get(1)).symbolType()) + "Consumer";
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

  @CheckForNull
  private static TypeArguments interfaceParameters(TypeTree interfaceType) {
    if (interfaceType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return ((ParameterizedTypeTree) interfaceType).typeArguments();
    }
    return null;
  }

  private void report(Tree tree, @Nullable String reportString) {
    if (reportString != null) {
      reportIssue(tree, REPORT_STRING + reportString + "'");
    }
  }

  private void report(Tree tree, List<String> reportMessages) {
    if (reportMessages.size() > 1) {
      StringBuilder reportMessage = new StringBuilder();
      reportMessage.append("Refactor this code to use the more specialised Functional Interfaces '");
      Iterator<String> it = reportMessages.iterator();
      while (true) {
        reportMessage.append(it.next());
        if (it.hasNext()) {
          reportMessage.append(", ");
        } else {
          break;
        }
      }
      reportMessage.append("'");
      reportIssue(tree, reportMessage.toString());
    } else {
      reportIssue(tree, REPORT_STRING + reportMessages.get(0) + "'");
    }
  }

  private static class ParameterTypeNameAndTreeType {
    Type paramType;
    String paramTypeName;
    String originalName;

    public ParameterTypeNameAndTreeType(Tree tree) {
      switch (tree.kind()) {
        case IDENTIFIER:
          IdentifierTree idTree = (IdentifierTree) tree;
          this.paramType = idTree.symbolType();
          this.paramTypeName = returnStringFromJavaObject(this.paramType);
          this.originalName = idTree.name();
          break;
        case MEMBER_SELECT:
          MemberSelectExpressionTree memberTree = (MemberSelectExpressionTree) tree;
          this.paramType = memberTree.symbolType();
          this.paramTypeName = returnStringFromJavaObject(this.paramType);
          this.originalName = memberTree.identifier().name();
          break;
        case PARAMETERIZED_TYPE:
          ParameterizedTypeTree p = (ParameterizedTypeTree) tree;
          this.paramTypeName = null;
          this.paramType = null;
          this.originalName = nameFromType(p);
          break;
        case EXTENDS_WILDCARD:
        case UNBOUNDED_WILDCARD:
        case SUPER_WILDCARD:
          this.paramTypeName = null;
          this.paramType = null;
          this.originalName = nameFromType(tree);
          break;
        default:
          break;
      }
    }

    private static String nameFromType(Tree tree) {
      String stringToReturn = null;
      if (tree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
        stringToReturn = nameFromParameterizedType((ParameterizedTypeTree) tree);
      } else if (tree.is(Tree.Kind.EXTENDS_WILDCARD, Tree.Kind.SUPER_WILDCARD, Tree.Kind.UNBOUNDED_WILDCARD)) {
        stringToReturn = nameFromWildcardType((WildcardTree) tree);
      }
      return stringToReturn;
    }

    private static String nameFromParameterizedType(ParameterizedTypeTree tree) {
      StringBuilder stringToReturn = new StringBuilder();
      stringToReturn.append((tree).type().symbolType().name() + "<");
      boolean firstParameter = true;
      for (Tree argument : (tree).typeArguments()) {
        if (argument.is(Tree.Kind.IDENTIFIER)) {
          stringToReturn.append(((IdentifierTree) argument).name());
        } else if (argument.is(Tree.Kind.MEMBER_SELECT)) {
          stringToReturn.append(((IdentifierTree) ((MemberSelectExpressionTree) argument).expression()).name() + "." +
            ((MemberSelectExpressionTree) argument).identifier().name());
        } else if (argument.is(Tree.Kind.PARAMETERIZED_TYPE)) {
          stringToReturn.append(nameFromParameterizedType((ParameterizedTypeTree) argument));
        } else if (argument.is(Tree.Kind.SUPER_WILDCARD, Tree.Kind.EXTENDS_WILDCARD, Tree.Kind.UNBOUNDED_WILDCARD)) {
          WildcardTree wildcardTree = (WildcardTree) argument;
          stringToReturn.append(wildcardTree.queryToken());
        }
        if (firstParameter) {
          stringToReturn.append(", ");
          firstParameter = false;
        } else {
          stringToReturn.append(">");
          firstParameter = true;
        }
      }
      return stringToReturn.toString();
    }

    private static String nameFromWildcardType(WildcardTree tree) {
      StringBuilder stringToReturn = new StringBuilder();
      WildcardTree wildcardTree = tree;
      stringToReturn.append(wildcardTree.queryToken().text());
      if (wildcardTree.extendsOrSuperToken() != null) {
        stringToReturn.append(" " + wildcardTree.extendsOrSuperToken().text() + " " + wildcardTree.bound().symbolType().name());
      }
      return stringToReturn.toString();
    }
  }
}
