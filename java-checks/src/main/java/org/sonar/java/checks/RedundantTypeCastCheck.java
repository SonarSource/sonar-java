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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.java.resolve.TypeVariableJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1905")
public class RedundantTypeCastCheck extends IssuableSubscriptionVisitor {

  private static final Predicate<Symbol.MethodSymbol> NON_DEFAULT_METHOD_PREDICATE = method -> !JUtils.isDefaultMethod(method);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    TypeCastTree typeCastTree = (TypeCastTree) tree;
    Type expressionType = typeCastTree.expression().symbolType();
    if (isPrimitiveWrapperInConditional(expressionType, typeCastTree) || requiredForMemberAccess(typeCastTree)) {
      // Primitive wrappers excluded because covered by S2154
      return;
    }
    Type cast = typeCastTree.type().symbolType();
    Type target = targetType(typeCastTree);
    if (target != null &&(isRedundantNumericalCast(cast, expressionType) || isUnnecessarySubtypeCast(expressionType, typeCastTree, target))) {
      reportIssue(typeCastTree.type(), "Remove this unnecessary cast to \"" + cast.erasure() + "\".");
    }
  }

  private static boolean requiredForMemberAccess(TypeCastTree typeCastTree) {
    ExpressionTree expression = typeCastTree.expression();
    if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
      Tree parent = typeCastTree.parent();
      return expression.is(Tree.Kind.METHOD_REFERENCE) && parent != null && skipParentheses(parent).is(Tree.Kind.MEMBER_SELECT);
    }
    Symbol symbol = ((MethodInvocationTree) expression).symbol();
    if (!symbol.isMethodSymbol()) {
      return false;
    }
    Type returnType = ((Symbol.MethodSymbol) symbol).returnType().type();
    if ((returnType instanceof TypeVariableJavaType) && ((TypeVariableJavaType) returnType).bounds().get(0).is("java.lang.Object")) {
      return false;
    }
    // consider REQUIRED as soon as the parent expression is a member access (killing the noise), without checking if cast could have been avoided
    // as the member accessed could have also been part of initial type
    return skipParentheses(typeCastTree.parent()).is(Tree.Kind.MEMBER_SELECT);
  }

  private static boolean isPrimitiveWrapperInConditional(Type expressionType, TypeCastTree typeCastTree) {
    Tree parent = skipParentheses(typeCastTree.parent());
    return parent.is(Tree.Kind.CONDITIONAL_EXPRESSION) && (JUtils.isPrimitiveWrapper(expressionType) || expressionType.isPrimitive());
  }

  @CheckForNull
  private static Type targetType(TypeCastTree typeCastTree) {
    Tree parent = skipParentheses(typeCastTree.parent());

    switch (parent.kind()) {
      case RETURN_STATEMENT:
        Tree method = parent;
        while (!method.is(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)) {
          method = method.parent();
        }
        if (method.is(Tree.Kind.METHOD)) {
          MethodTree methodTree = (MethodTree) method;
          Type type = methodTree.symbol().type();
          if (type instanceof MethodJavaType) {
            return ((MethodJavaType) type).resultType();
          }
          return methodTree.symbol().returnType().type();
        }
        return null;
      case VARIABLE:
        return ((VariableTree) parent).symbol().type();
      case ARGUMENTS:
        Arguments arguments = (Arguments)parent;
        int castArgIndex = arguments.indexOf(typeCastTree);
        return arguments.get(castArgIndex).symbolType();
      case MEMBER_SELECT:
      case CONDITIONAL_EXPRESSION:
        return typeCastTree.type().symbolType();
      case ARRAY_ACCESS_EXPRESSION:
        return ((ArrayAccessExpressionTree) parent).expression().symbolType();
      default:
        if (parent instanceof ExpressionTree) {
          return ((ExpressionTree) parent).symbolType();
        }
        return null;
    }
  }

  private static Tree skipParentheses(Tree parent) {
    Tree skip = parent;
    while (skip.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      skip = skip.parent();
    }
    return skip;
  }

  private static boolean isUnnecessarySubtypeCast(Type childType, TypeCastTree typeCastTree, Type parentType) {
    boolean isArgument = skipParentheses(typeCastTree.parent()).is(Tree.Kind.ARGUMENTS);
    return !childType.isPrimitive()
      // Exception: subtype cast are tolerated in method or constructor call arguments
      && ((isArgument && childType.equals(parentType)) || (!isArgument && childType.isSubtypeOf(parentType)))
      && (!ExpressionUtils.skipParentheses(typeCastTree.expression()).is(Tree.Kind.LAMBDA_EXPRESSION)
        || isUnnecessaryLambdaCast(childType, parentType));
  }

  private static boolean isUnnecessaryLambdaCast(Type childType, Type parentType) {
    if (parentType.isSubtypeOf(childType) && !isRawTypeOfParameterizedType(parentType, childType)) {
      return true;
    }
    // intersection type on lambda should not raise an issue : required to make lambda serializable for instance
    if (JUtils.isIntersectionType(childType)) {
      return false;
    }

    List<Symbol.MethodSymbol> childMethods = getMethodSymbolsOf(childType).collect(Collectors.toList());
    return childMethods.isEmpty() || (childMethods.size() == 1 && isSingleAbstractMethodOverride(childMethods.get(0), parentType));
  }

  private static boolean isRawTypeOfParameterizedType(Type parentType, Type childType) {
    return JUtils.isParametrized(childType)
      && !JUtils.isParametrized(parentType)
      && parentType.erasure().equals(childType.erasure());
  }

  private static boolean isSingleAbstractMethodOverride(Symbol.MethodSymbol childMethod, Type parentType) {
    Symbol.MethodSymbol overriddenSymbol = childMethod.overriddenSymbol();
    return !JUtils.isDefaultMethod(childMethod)
      && overriddenSymbol != null
      && getMethodSymbolsOf(parentType).filter(NON_DEFAULT_METHOD_PREDICATE).anyMatch(overriddenSymbol::equals);
  }

  private static Stream<Symbol.MethodSymbol> getMethodSymbolsOf(Type type) {
    return type.symbol().memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast);
  }

  private static boolean isRedundantNumericalCast(Type cast, Type expressionType) {
    return cast.isNumerical() && cast.equals(expressionType);
  }

}
