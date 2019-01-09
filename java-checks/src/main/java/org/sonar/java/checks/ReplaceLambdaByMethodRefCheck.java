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

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1612")
public class ReplaceLambdaByMethodRefCheck extends BaseTreeVisitor implements JavaFileScanner, JavaVersionAwareVisitor {

  private JavaFileScannerContext context;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    if (isReplaceableSingleMethodInvocation(tree) || isBodyBlockInvokingMethod(tree)) {
      context.reportIssue(this, tree.arrowToken(), "Replace this lambda with a method reference." + context.getJavaVersion().java8CompatibilityMessage());
    } else {
      getNullCheck(tree)
        .ifPresent(nullMethod -> 
          context.reportIssue(this, tree.arrowToken(),
            "Replace this lambda with method reference 'Objects::" + nullMethod + "'." + context.getJavaVersion().java8CompatibilityMessage())
      );
    }
    super.visitLambdaExpression(tree);
  }

  private static Optional<String> getNullCheck(LambdaExpressionTree lambda) {
    Tree lambdaBody = lambda.body();
    if (isBlockWithOneStatement(lambdaBody)) {
      return getNullCheckFromReturn(((BlockTree) lambdaBody).body().get(0), lambda);
    }
    return getNullCheck(lambdaBody, lambda);
  }

  private static Optional<String> getNullCheckFromReturn(Tree statement, LambdaExpressionTree lambda) {
    if (statement.is(Tree.Kind.RETURN_STATEMENT)) {
      return getNullCheck(((ReturnStatementTree) statement).expression(), lambda);
    }
    return Optional.empty();
  }

  private static Optional<String> getNullCheck(@Nullable Tree statement, LambdaExpressionTree tree) {
    if (statement == null) {
      return Optional.empty();
    }
    Tree expr = statement;
    if (expr.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      expr = ExpressionUtils.skipParentheses((ParenthesizedTree) statement);
    }
    if (expr.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      BinaryExpressionTree bet = (BinaryExpressionTree) expr;
      ExpressionTree leftOperand = ExpressionUtils.skipParentheses(bet.leftOperand());
      ExpressionTree rightOperand = ExpressionUtils.skipParentheses(bet.rightOperand());
      if (nullAgainstParam(leftOperand, rightOperand, tree) || nullAgainstParam(rightOperand, leftOperand, tree)) {
        return Optional.of(expr.is(Tree.Kind.EQUAL_TO) ? "isNull" : "nonNull");
      }
    }
    return Optional.empty();
  }

  private static boolean nullAgainstParam(ExpressionTree o1, ExpressionTree o2, LambdaExpressionTree tree) {
    if (o1.is(Tree.Kind.NULL_LITERAL) && o2.is(Tree.Kind.IDENTIFIER)) {
      List<VariableTree> parameters = tree.parameters();
      return parameters.size() == 1 && parameters.get(0).symbol().equals(((IdentifierTree) o2).symbol());
    }
    return false;
  }

  private static boolean isReplaceableSingleMethodInvocation(LambdaExpressionTree lambdaTree) {
    return isMethodInvocation(lambdaTree.body(), lambdaTree);
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
      if(tree.is(Tree.Kind.NEW_CLASS)) {
        if(((NewClassTree) tree).classBody() != null) {
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
      return expression.is(Tree.Kind.IDENTIFIER) && parameters.get(0).symbol().equals(((IdentifierTree) expression).symbol());
    }
    return false;
  }
}
