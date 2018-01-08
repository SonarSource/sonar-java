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

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Optional;

@Rule(key = "S1698")
public class CompareObjectWithEqualsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final MethodMatcher EQUALS_MATCHER = MethodMatcher.create().name("equals").parameters(JAVA_LANG_OBJECT);
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (!isEquals(tree)) {
      super.visitMethod(tree);
    }
  }

  private static boolean isEquals(MethodTree tree) {
    return ((MethodTreeImpl) tree).isEqualsMethod();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      Type leftOpType = tree.leftOperand().symbolType();
      Type rightOpType = tree.rightOperand().symbolType();
      if (!isExcluded(leftOpType, rightOpType) && hasObjectOperand(leftOpType, rightOpType)
        && bothImplementsEqualsMethod(leftOpType, rightOpType)
        && neitherIsPublicStaticFinal(tree.leftOperand(), tree.rightOperand())) {
        context.reportIssue(this, tree.operatorToken(), "Use the \"equals\" method if value comparison was intended.");
      }
    }
  }

  private static boolean neitherIsPublicStaticFinal(ExpressionTree leftOperand, ExpressionTree rightOperand) {
    if (compatibleTypes(leftOperand, rightOperand)) {
      return !isPublicStaticFinal(leftOperand) && !isPublicStaticFinal(rightOperand);
    }
    return true;
  }

  private static boolean compatibleTypes(ExpressionTree leftOperand, ExpressionTree rightOperand) {
    return leftOperand.symbolType().equals(rightOperand.symbolType());
  }

  private static boolean isPublicStaticFinal(ExpressionTree tree) {
    return symbol(tree)
      .map(s -> s.isPublic() && s.isStatic() && s.isFinal())
      .orElse(false);
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
      || isObjectType(leftOpType, rightOpType);
  }

  private static boolean isObjectType(Type leftOpType, Type rightOpType) {
    return leftOpType.is(JAVA_LANG_OBJECT) || rightOpType.is(JAVA_LANG_OBJECT);
  }

  private static boolean isObject(Type operandType) {
    return operandType.erasure().isClass() && !operandType.symbol().isEnum();
  }

  private static boolean isNullComparison(Type leftOpType, Type rightOpType) {
    return isBot(leftOpType) || isBot(rightOpType);
  }

  private static boolean isNumericalComparison(Type leftOperandType, Type rightOperandType) {
    return leftOperandType.isNumerical() || rightOperandType.isNumerical();
  }

  private static boolean isJavaLangClassComparison(Type leftOpType, Type rightOpType) {
    return leftOpType.is("java.lang.Class") || rightOpType.is("java.lang.Class");
  }

  private static boolean isBot(Type type) {
    return ((JavaType) type).isTagged(JavaType.BOT);
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
