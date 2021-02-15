/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1612")
public class ReplaceLambdaByMethodRefCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    visitLambdaExpression((LambdaExpressionTree) tree);
  }

  private void visitLambdaExpression(LambdaExpressionTree tree) {
    if (isMethodInvocation(tree.body(), tree) || isBodyBlockInvokingMethod(tree)) {
      reportIssue(tree.arrowToken(), "Replace this lambda with a method reference." + context.getJavaVersion().java8CompatibilityMessage());
    } else {
      getTypeCastOrInstanceOf(tree)
        .ifPresent(methodReference -> reportIssue(tree.arrowToken(),
          "Replace this lambda with method reference '" + methodReference + "'." + context.getJavaVersion().java8CompatibilityMessage()));
      getNullCheck(tree)
        .ifPresent(nullMethod -> reportIssue(tree.arrowToken(),
          "Replace this lambda with method reference 'Objects::" + nullMethod + "'." + context.getJavaVersion().java8CompatibilityMessage()));
    }
  }

  private static Optional<String> getNullCheck(LambdaExpressionTree lambda) {
    return getLambdaSingleParamSymbol(lambda).flatMap(symbol -> {
      Tree lambdaBody = lambda.body();
      return (isBlockWithOneStatement(lambdaBody)) ? 
        getNullCheckFromReturn(((BlockTree) lambdaBody).body().get(0), symbol) :
        getNullCheck(lambdaBody, symbol);
    });
  }

  private static Optional<String> getNullCheckFromReturn(Tree statement, Symbol paramSymbol) {
    return statement.is(Tree.Kind.RETURN_STATEMENT) ?
      getNullCheck(((ReturnStatementTree) statement).expression(), paramSymbol) :
      Optional.empty();
  }

  private static Optional<String> getNullCheck(@Nullable Tree statement, Symbol paramSymbol) {
    return expressionWithoutParentheses(statement).flatMap(expr -> {
      if (expr.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
        BinaryExpressionTree bet = (BinaryExpressionTree) expr;
        ExpressionTree leftOperand = ExpressionUtils.skipParentheses(bet.leftOperand());
        ExpressionTree rightOperand = ExpressionUtils.skipParentheses(bet.rightOperand());
        if (nullAgainstParam(leftOperand, rightOperand, paramSymbol) || nullAgainstParam(rightOperand, leftOperand, paramSymbol)) {
          return Optional.of(expr.is(Tree.Kind.EQUAL_TO) ? "isNull" : "nonNull");
        }
      }
      return Optional.empty();
    });
  }

  private static boolean nullAgainstParam(ExpressionTree o1, ExpressionTree o2, Symbol paramSymbol) {
    return o1.is(Tree.Kind.NULL_LITERAL) &&
      o2.is(Tree.Kind.IDENTIFIER) && 
      paramSymbol.equals(((IdentifierTree) o2).symbol());
  }

  private static Optional<String> getTypeCastOrInstanceOf(LambdaExpressionTree lambda) {
    return getLambdaSingleParamSymbol(lambda).flatMap(symbol -> {
      Tree lambdaBody = lambda.body();
      return isBlockWithOneStatement(lambdaBody) ? 
        getTypeCastOrInstanceOfFromReturn(((BlockTree) lambdaBody).body().get(0), symbol) : 
        getTypeCastOrInstanceOf(lambdaBody, symbol);
    });
  }

  private static Optional<String> getTypeCastOrInstanceOfFromReturn(Tree statement, Symbol symbol) {
    return statement.is(Tree.Kind.RETURN_STATEMENT) ?
      getTypeCastOrInstanceOf(((ReturnStatementTree) statement).expression(), symbol) :
      Optional.empty();
  }

  private static Optional<String> getTypeCastOrInstanceOf(@Nullable Tree statement, Symbol symbol) {
    return statement == null ?
      Optional.empty() :
      expressionWithoutParentheses(statement).flatMap(expr -> getTypeCastOrInstanceOfName(symbol, expr));
  }

  private static Optional<String> getTypeCastOrInstanceOfName(Symbol symbol, ExpressionTree expr) {
    if (expr.is(Tree.Kind.TYPE_CAST)) {
      TypeCastTree typeCastTree = (TypeCastTree) expr;
      if (isSingleParamExpression(typeCastTree.expression(), symbol)) {
        return getTypeName(typeCastTree.type())
          .map(s -> s + ".class::cast");
      }
    } else if (expr.is(Tree.Kind.INSTANCE_OF)) {
      InstanceOfTree instanceOfTree = (InstanceOfTree) expr;
      if (isSingleParamExpression(instanceOfTree.expression(), symbol)) {
        return getTypeName(instanceOfTree.type())
          .map(s -> s + ".class::isInstance");
      }
    } 
    return Optional.empty();
  }
  
  private static Optional<String> getTypeName(TypeTree type) {
    if (type.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      type = ((ParameterizedTypeTree) type).type();
    }
    if (type.is(Tree.Kind.IDENTIFIER) && !isGeneric((IdentifierTree) type)) {
      return Optional.of(((IdentifierTree) type).name());
    }
    if (type.is(Tree.Kind.ARRAY_TYPE)) {
      return getTypeName(((ArrayTypeTree) type).type()).map(x -> x + "[]");
    }
    if (type.is(Tree.Kind.PRIMITIVE_TYPE)) {
      return Optional.of(((PrimitiveTypeTree) type).keyword().text());
    }
    return Optional.empty();
  }

  private static boolean isGeneric(IdentifierTree identifierTree) {
    return JUtils.isTypeVar(identifierTree.symbolType());
  }
  
  private static boolean isSingleParamExpression(ExpressionTree expression, Symbol symbol) {
    return expression.is(Tree.Kind.IDENTIFIER) && symbol.equals(((IdentifierTree) expression).symbol());
  }

  private static Optional<Symbol> getLambdaSingleParamSymbol(LambdaExpressionTree tree) {
    List<VariableTree> parameters = tree.parameters();
    return parameters.size() == 1 ? Optional.of(parameters.get(0).symbol()) : Optional.empty();
  }

  private static boolean isBodyBlockInvokingMethod(LambdaExpressionTree lambdaTree) {
    Tree lambdaBody = lambdaTree.body();
    if (isBlockWithOneStatement(lambdaBody)) {
      Tree statement = ((BlockTree) lambdaBody).body().get(0);
      return isExpressionStatementInvokingMethod(statement, lambdaTree) || isReturnStatementInvokingMethod(statement, lambdaTree);
    }
    return false;
  }

  private static boolean isBlockWithOneStatement(Tree tree) {
    return tree.is(Tree.Kind.BLOCK) && ((BlockTree) tree).body().size() == 1;
  }

  private static boolean isExpressionStatementInvokingMethod(Tree statement, LambdaExpressionTree lambdaTree) {
    return statement.is(Tree.Kind.EXPRESSION_STATEMENT) && isMethodInvocation(((ExpressionStatementTree) statement).expression(), lambdaTree);
  }

  private static boolean isReturnStatementInvokingMethod(Tree statement, LambdaExpressionTree lambdaTree) {
    return statement.is(Tree.Kind.RETURN_STATEMENT) && isMethodInvocation(((ReturnStatementTree) statement).expression(), lambdaTree);
  }

  private static boolean isMethodInvocation(@Nullable Tree tree, LambdaExpressionTree lambdaTree) {
    if (tree != null && tree.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS)) {
      Arguments arguments;
      if (tree.is(Tree.Kind.NEW_CLASS)) {
        if (((NewClassTree) tree).classBody() != null) {
          return false;
        }
        arguments = ((NewClassTree) tree).arguments();
      } else {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        if (hasMethodInvocationInMethodSelect(mit) || hasNonFinalFieldInMethodSelect(mit)) {
          return false;
        }
        arguments = mit.arguments();
      }
      List<VariableTree> parameters = lambdaTree.parameters();
      return matchingParameters(parameters, arguments) || (arguments.isEmpty() && isNoArgMethodInvocationFromLambdaParam(tree, parameters));
    }
    return false;
  }

  private static boolean hasMethodInvocationInMethodSelect(MethodInvocationTree mit) {
    MemberSelectExpressionTree mse = getMemberSelect(mit);
    while (mse != null) {
      ExpressionTree expression = mse.expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS)) {
        return true;
      }
      if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        mse = (MemberSelectExpressionTree) expression;
      } else {
        mse = null;
      }
    }
    return false;
  }

  @CheckForNull
  private static MemberSelectExpressionTree getMemberSelect(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (!methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      return null;
    }
    return (MemberSelectExpressionTree) methodSelect;
  }

  private static boolean hasNonFinalFieldInMethodSelect(MethodInvocationTree mit) {
    MemberSelectExpressionTree mse = getMemberSelect(mit);
    if (mse == null) {
      return false;
    }
    ExpressionTree expression = ExpressionUtils.skipParentheses(mse.expression());
    Symbol symbol = null;
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      symbol = ((IdentifierTree) expression).symbol();
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      symbol = ((MemberSelectExpressionTree) expression).identifier().symbol();
    }
    return symbol != null &&
      symbol.owner().isTypeSymbol()
      && !isThisOrSuper(symbol.name())
      && !symbol.isFinal();
  }

  private static boolean isThisOrSuper(String name) {
    return "this".equals(name) || "super".equals(name);
  }

  private static boolean matchingParameters(List<VariableTree> parameters, Arguments arguments) {
    return arguments.size() == parameters.size() &&
      IntStream.range(0, arguments.size()).allMatch(i -> {
        List<IdentifierTree> usages = parameters.get(i).symbol().usages();
        return usages.size() == 1 && usages.get(0).equals(arguments.get(i));
      });
  }

  private static boolean isNoArgMethodInvocationFromLambdaParam(Tree tree, List<VariableTree> parameters) {
    if (!tree.is(Tree.Kind.METHOD_INVOCATION) || parameters.size() != 1) {
      return false;
    }
    ExpressionTree methodSelect = ((MethodInvocationTree) tree).methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      Symbol parameterSymbol = parameters.get(0).symbol();
      return expression.is(Tree.Kind.IDENTIFIER) &&
        !parameterSymbol.isUnknown() &&
        !isAmbiguous((MethodInvocationTree) tree, parameterSymbol) &&
        parameterSymbol.equals(((IdentifierTree) expression).symbol());
    }
    return false;
  }

  public static Optional<ExpressionTree> expressionWithoutParentheses(@Nullable Tree tree) {
    if (!(tree instanceof ExpressionTree)) {
      return Optional.empty();
    }
    ExpressionTree result = ((ExpressionTree) tree);
    return Optional.of(ExpressionUtils.skipParentheses(result));
  }

  /**
   * This is a crude way to shutdown the FPs when method reference is ambiguous in case of lambda like x -> x.foo()
   * Full resolution algorithm is described in JLS 15.13.1
   */
  private static boolean isAmbiguous(MethodInvocationTree tree, Symbol parameterSymbol) {
    Symbol method = tree.symbol();
    Symbol.TypeSymbol methodOwner = (Symbol.TypeSymbol) method.owner();
    if (methodOwner.isUnknown() || method.isUnknown()) {
      return true;
    }
    // suitable method is instance method with no parameters, or static method with single parameter of the same type as lambda argument
    return methodOwner.lookupSymbols(method.name())
      .stream()
      .filter(Symbol::isMethodSymbol)
      .map(s -> (Symbol.MethodSymbol) s)
      .filter(m -> (!m.isStatic() && m.parameterTypes().isEmpty())
        || (m.isStatic() && m.parameterTypes().size() == 1 && parameterSymbol.type().isSubtypeOf(m.parameterTypes().get(0))))
      .count() > 1;
  }
}
