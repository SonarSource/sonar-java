/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
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
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
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
    getPossibleReplacement(tree)
      .filter(replacement -> isReplacementMoreConcise(tree, replacement))
      .ifPresent(replacement ->
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(tree.arrowToken())
          .withMessage("Replace this lambda with method reference '%s'.%s", replacement, context.getJavaVersion().java8CompatibilityMessage())
          .withQuickFix(() -> JavaQuickFix.newQuickFix("Replace with \"%s\"", replacement)
            .addTextEdit(JavaTextEdit.replaceTree(tree, replacement))
            .build())
          .report()
      );
  }

  private static boolean isReplacementMoreConcise(LambdaExpressionTree tree, String replacement) {
    SyntaxToken first = Objects.requireNonNull(tree.firstToken());
    SyntaxToken last = Objects.requireNonNull(tree.lastToken());
    boolean multiline = first.range().start().line() != last.range().end().line();
    boolean shorter = replacement.length() <= last.range().end().column() - first.range().start().column();
    return multiline || shorter;
  }

  private static Optional<String> getPossibleReplacement(LambdaExpressionTree tree) {
    Optional<String> typeCastOrInstanceOf = getTypeCastOrInstanceOf(tree);
    if (typeCastOrInstanceOf.isPresent()) {
      return typeCastOrInstanceOf;
    }
    Optional<String> nullCheck = getNullCheck(tree);
    if (nullCheck.isPresent()) {
      return nullCheck;
    }
    Optional<String> methodInvocationOrNewClass = getMethodInvocationOrNewClass(tree.body(), tree);
    if (methodInvocationOrNewClass.isPresent()) {
      return methodInvocationOrNewClass;
    }
    return getBodyBlockInvokingMethodOrNewClass(tree);
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
          return Optional.of(expr.is(Tree.Kind.EQUAL_TO) ? "Objects::isNull" : "Objects::nonNull");
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
      if (isSingleParamExpression(typeCastTree.expression(), symbol) && !symbol.type().isPrimitive()) {
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

  private static Optional<String> getBodyBlockInvokingMethodOrNewClass(LambdaExpressionTree lambdaTree) {
    Tree lambdaBody = lambdaTree.body();
    if (isBlockWithOneStatement(lambdaBody)) {
      Tree statement = ((BlockTree) lambdaBody).body().get(0);
      return getExpressionOrReturnStatementInvokingMethod(statement, lambdaTree);
    }
    return Optional.empty();
  }

  private static boolean isBlockWithOneStatement(Tree tree) {
    return tree.is(Tree.Kind.BLOCK) && ((BlockTree) tree).body().size() == 1;
  }

  private static Optional<String> getExpressionOrReturnStatementInvokingMethod(Tree statement, LambdaExpressionTree lambdaTree) {
    if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return getMethodInvocationOrNewClass(((ExpressionStatementTree) statement).expression(), lambdaTree);
    } else if (statement.is(Tree.Kind.RETURN_STATEMENT)) {
      return getMethodInvocationOrNewClass(((ReturnStatementTree) statement).expression(), lambdaTree);
    }
    return Optional.empty();
  }

  private static Optional<String> getMethodInvocationOrNewClass(@Nullable Tree tree, LambdaExpressionTree lambdaTree) {
    if (tree != null) {
      List<VariableTree> parameters = lambdaTree.parameters();

      if (tree.is(Tree.Kind.NEW_CLASS)) {
        // x -> new Foo(x) becomes Foo::new
        return getNewClass(((NewClassTree) tree), parameters);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        if (mit.methodSymbol().isUnknown() || hasMethodInvocationInMethodSelect(mit) || hasNonFinalFieldInMethodSelect(mit)) {
          return Optional.empty();
        }
        if (matchingParameters(mit.arguments(), parameters)) {
          // x -> foo(x) becomes y::foo or Owner::foo or this::foo or Owner.this::foo
          return getReplacementForMethodInvocation(mit);
        }
        if (isMethodCalledOnFirstParam(mit, parameters)) {
          // x -> x.foo() becomes Owner::foo
          return getUnambiguousReference(mit);
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<String> getUnambiguousReference(MethodInvocationTree mit) {
    Symbol.MethodSymbol ms = mit.methodSymbol();
    ArrayList<Symbol.MethodSymbol> methodSymbols = new ArrayList<>(ms.overriddenSymbols());
    Collections.reverse(methodSymbols);
    methodSymbols.add(ms);
    return methodSymbols.stream()
      .filter(m -> m.thrownTypes().equals(ms.thrownTypes()))
      .filter(m -> !hasAmbiguousReference(m))
      .findFirst()
      .map(ReplaceLambdaByMethodRefCheck::getMethodReferenceFromSymbol);
  }

  private static boolean hasAmbiguousReference(Symbol.MethodSymbol ms) {
    return ((Symbol.TypeSymbol) ms.owner()).lookupSymbols(ms.name()).stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast)
      .filter(m -> m.isStatic() != ms.isStatic())
      .anyMatch(m -> m.isStatic() ? methodsHaveSameReference(m, ms) : methodsHaveSameReference(ms, m));
  }

  private static boolean methodsHaveSameReference(Symbol.MethodSymbol mStatic, Symbol.MethodSymbol mNotStatic) {
    Type ownerType = mStatic.owner().type();
    return mStatic.parameterTypes().size() == mNotStatic.parameterTypes().size() + 1 &&
           isArgumentCompatible(ownerType, mStatic.parameterTypes().get(0));
  }

  private static Optional<String> getNewClass(NewClassTree newClassTree, List<VariableTree> parameters) {
    if (newClassTree.classBody() == null && matchingParameters(newClassTree.arguments(), parameters)) {
      TypeTree identifier = newClassTree.identifier();
      if (identifier.is(Tree.Kind.MEMBER_SELECT, Tree.Kind.IDENTIFIER)) {
        String className = ExpressionsHelper.concatenate((ExpressionTree) identifier);
        return Optional.of(className + "::new");
      }
    }
    return Optional.empty();
  }

  private static Optional<String> getReplacementForMethodInvocation(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    Symbol.MethodSymbol symbol = mit.methodSymbol();
    if (symbol.isStatic()) {
      return getUnambiguousReference(mit);
    }
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(mit);
      Symbol symbolOwner = symbol.owner();
      if (enclosingMethod != null) {
        Symbol expressionOwner = enclosingMethod.symbol().owner();
        if (symbolOwner.equals(expressionOwner)) {
          return Optional.of("this::" + symbol.name());
        }
      }
      return Optional.of(symbolOwner.name() + ".this::" + symbol.name());
    }
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
    return Optional.of(ExpressionsHelper.concatenate(memberSelect.expression()) + "::" + memberSelect.identifier().name());
  }

  private static String getMethodReferenceFromSymbol(Symbol symbol) {
    return symbol.owner().name() + "::" + symbol.name();
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

  private static boolean matchingParameters(Arguments arguments, List<VariableTree> parameters) {
    return arguments.size() == parameters.size() &&
      IntStream.range(0, arguments.size()).allMatch(i -> {
        List<IdentifierTree> usages = parameters.get(i).symbol().usages();
        return usages.size() == 1 && usages.get(0).equals(arguments.get(i));
      });
  }

  /**
   * Checks if single expression lambda is a method call on its first parameter, and all
   * other parameters are used in-order as arguments in said method call
   * Example: (a, b, c) -> a.foo(b, c)
   */
  private static boolean isMethodCalledOnFirstParam(MethodInvocationTree mit, List<VariableTree> parameters) {
    if (!parameters.isEmpty() && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      Symbol parameterSymbol = parameters.get(0).symbol();
      return expression.is(Tree.Kind.IDENTIFIER) &&
        !parameterSymbol.isUnknown() &&
        parameterSymbol.equals(((IdentifierTree) expression).symbol()) &&
        matchingParameters(mit.arguments(), parameters.subList(1, parameters.size()));
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

  private static boolean isArgumentCompatible(Type argumentType, Type parameterType) {
    return argumentType.isSubtypeOf(parameterType)
      || JUtils.wrapTypeIfPrimitive(argumentType).equals(JUtils.wrapTypeIfPrimitive(parameterType));
  }
}
