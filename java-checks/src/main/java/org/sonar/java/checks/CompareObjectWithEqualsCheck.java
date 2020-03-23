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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;

@Rule(key = "S1698")
public class CompareObjectWithEqualsCheck extends CompareWithEqualsVisitor {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final MethodMatchers EQUALS_MATCHER = MethodMatchers.create().ofAnyType().names("equals").addParametersMatcher(JAVA_LANG_OBJECT).build();

  @Override
  protected void checkEqualityExpression(BinaryExpressionTree tree) {
    ExpressionTree leftExpression = tree.leftOperand();
    ExpressionTree rightExpression = tree.rightOperand();
    Type leftOpType = leftExpression.symbolType();
    Type rightOpType = rightExpression.symbolType();
    if (!isExcluded(leftOpType, rightOpType) && hasObjectOperand(leftOpType, rightOpType)
      && neitherIsThis(leftExpression, rightExpression)
      && bothImplementsEqualsMethod(leftOpType, rightOpType)
      && neitherIsPublicStaticFinal(leftExpression, rightExpression)) {
      reportIssue(tree.operatorToken());
    }
  }

  private static boolean neitherIsThis(ExpressionTree leftExpression, ExpressionTree rightExpression) {
    return !ExpressionUtils.isThis(leftExpression) && !ExpressionUtils.isThis(rightExpression);
  }

  private static boolean neitherIsPublicStaticFinal(ExpressionTree leftOperand, ExpressionTree rightOperand) {
    if (compatibleTypes(leftOperand, rightOperand)) {
      return !isFinal(leftOperand) && !isFinal(rightOperand);
    }
    return true;
  }

  private static boolean compatibleTypes(ExpressionTree leftOperand, ExpressionTree rightOperand) {
    return leftOperand.symbolType().equals(rightOperand.symbolType());
  }

  private static boolean isFinal(ExpressionTree tree) {
    return symbol(tree)
      .filter(Symbol::isFinal)
      .isPresent();
  }

  private static Optional<Symbol> symbol(ExpressionTree tree) {
    switch (tree.kind()) {
      case IDENTIFIER:
        return Optional.of(((IdentifierTree) tree).symbol());
      case MEMBER_SELECT:
        return Optional.of(((MemberSelectExpressionTree) tree).identifier().symbol());
      default:
        return Optional.empty();
    }
  }

  private static boolean hasObjectOperand(Type leftOpType, Type rightOpType) {
    return isObject(leftOpType) || isObject(rightOpType);
  }

  private static boolean isExcluded(Type leftOpType, Type rightOpType) {
    return isNullComparison(leftOpType, rightOpType)
      || isNumericalComparison(leftOpType, rightOpType)
      || isJavaLangClassComparison(leftOpType, rightOpType)
      || isObjectType(leftOpType, rightOpType)
      || isStringType(leftOpType, rightOpType)
      || isBoxedType(leftOpType, rightOpType);
  }

  private static boolean isObjectType(Type leftOpType, Type rightOpType) {
    return leftOpType.is(JAVA_LANG_OBJECT) || rightOpType.is(JAVA_LANG_OBJECT);
  }

  private static boolean isObject(Type operandType) {
    return operandType.erasure().isClass() && !operandType.symbol().isEnum();
  }

  private static boolean isNumericalComparison(Type leftOperandType, Type rightOperandType) {
    return leftOperandType.isNumerical() || rightOperandType.isNumerical();
  }

  private static boolean isJavaLangClassComparison(Type leftOpType, Type rightOpType) {
    return leftOpType.is("java.lang.Class") || rightOpType.is("java.lang.Class");
  }

  private static boolean bothImplementsEqualsMethod(Type leftOpType, Type rightOpType) {
    return implementsEqualsMethod(leftOpType) && implementsEqualsMethod(rightOpType);
  }

  private static boolean implementsEqualsMethod(Type type) {
    Symbol.TypeSymbol symbol = type.symbol();
    return hasEqualsMethod(symbol) || parentClassImplementsEquals(symbol);
  }

  private static boolean parentClassImplementsEquals(Symbol.TypeSymbol symbol) {
    Type superClass = symbol.superClass();
    while (superClass != null && superClass.symbol().isTypeSymbol()) {
      Symbol.TypeSymbol superClassSymbol = superClass.symbol();
      if (!superClass.is(JAVA_LANG_OBJECT) && hasEqualsMethod(superClassSymbol)) {
        return true;
      }
      superClass = superClassSymbol.superClass();
    }
    return false;
  }

  private static boolean hasEqualsMethod(Symbol.TypeSymbol symbol) {
    return symbol.lookupSymbols("equals").stream().anyMatch(EQUALS_MATCHER::matches);
  }
}
