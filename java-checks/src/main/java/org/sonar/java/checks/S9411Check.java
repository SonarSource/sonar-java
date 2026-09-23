/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.AbstractForLoopRule.ForLoopIncrement;
import org.sonar.java.checks.AbstractForLoopRule.ForLoopInitializer;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S9411")
public class S9411Check extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Replace this loop with \"List.replaceAll()\".";
  private static final String LIST_TYPE = "java.util.List";

  private static final MethodMatchers LIST_SET = MethodMatchers.create()
    .ofSubTypes(LIST_TYPE)
    .names("set")
    .addParametersMatcher("int", ANY)
    .build();

  private static final MethodMatchers LIST_GET = MethodMatchers.create()
    .ofSubTypes(LIST_TYPE)
    .names("get")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers LIST_SIZE = MethodMatchers.create()
    .ofSubTypes(LIST_TYPE)
    .names("size")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForStatementTree forStatement = (ForStatementTree) tree;

    ForLoopInitializer initializer = extractSingleInitializer(forStatement);
    if (initializer == null || !Integer.valueOf(0).equals(initializer.value())) {
      return;
    }

    ForLoopIncrement increment = ForLoopIncrement.findInUpdates(forStatement);
    if (increment == null || !increment.hasValue() || increment.value() != 1
      || !increment.hasSameIdentifier(initializer.identifier())) {
      return;
    }

    Symbol listSymbol = extractListSymbolFromCondition(forStatement, initializer);
    if (listSymbol == null) {
      return;
    }

    MethodInvocationTree setCall = extractSingleSetCall(forStatement, listSymbol, initializer);
    if (setCall == null) {
      return;
    }

    ExpressionTree setValue = setCall.arguments().get(1);
    if (containsListGetWithSameIndexAndList(setValue, listSymbol, initializer)) {
      reportIssue(forStatement.forKeyword(), MESSAGE);
    }
  }

  @CheckForNull
  private static ForLoopInitializer extractSingleInitializer(ForStatementTree forStatement) {
    List<StatementTree> initStatements = forStatement.initializer();
    if (initStatements.size() != 1 || !initStatements.get(0).is(Tree.Kind.VARIABLE)) {
      return null;
    }
    List<ForLoopInitializer> initializers = StreamSupport
      .stream(ForLoopInitializer.list(forStatement).spliterator(), false)
      .toList();
    if (initializers.size() != 1) {
      return null;
    }
    return initializers.get(0);
  }

  @CheckForNull
  private static Symbol extractListSymbolFromCondition(ForStatementTree forStatement, ForLoopInitializer initializer) {
    ExpressionTree condition = forStatement.condition();
    if (condition == null) {
      return null;
    }
    if (condition.is(Tree.Kind.LESS_THAN)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) condition;
      if (initializer.hasSameIdentifier(binary.leftOperand()) && isSizeCall(binary.rightOperand())) {
        return extractReceiverSymbol((MethodInvocationTree) binary.rightOperand());
      }
    }
    if (condition.is(Tree.Kind.GREATER_THAN)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) condition;
      if (isSizeCall(binary.leftOperand()) && initializer.hasSameIdentifier(binary.rightOperand())) {
        return extractReceiverSymbol((MethodInvocationTree) binary.leftOperand());
      }
    }
    return null;
  }

  private static boolean isSizeCall(ExpressionTree expression) {
    return expression.is(Tree.Kind.METHOD_INVOCATION) && LIST_SIZE.matches((MethodInvocationTree) expression);
  }

  @CheckForNull
  private static Symbol extractReceiverSymbol(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree receiver = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (receiver.is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) receiver).symbol();
        return symbol.isUnknown() ? null : symbol;
      }
    }
    return null;
  }

  @CheckForNull
  private static MethodInvocationTree extractSingleSetCall(ForStatementTree forStatement, Symbol listSymbol, ForLoopInitializer initializer) {
    StatementTree body = forStatement.statement();
    List<StatementTree> statements;
    if (body.is(Tree.Kind.BLOCK)) {
      statements = ((BlockTree) body).body();
    } else {
      statements = Collections.singletonList(body);
    }
    if (statements.size() != 1) {
      return null;
    }
    StatementTree singleStatement = statements.get(0);
    if (!singleStatement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return null;
    }
    ExpressionTree expression = ((ExpressionStatementTree) singleStatement).expression();
    if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    MethodInvocationTree mit = (MethodInvocationTree) expression;
    if (!LIST_SET.matches(mit)) {
      return null;
    }
    Symbol setReceiver = extractReceiverSymbol(mit);
    if (!listSymbol.equals(setReceiver)) {
      return null;
    }
    ExpressionTree indexArg = mit.arguments().get(0);
    if (!initializer.hasSameIdentifier(indexArg)) {
      return null;
    }
    return mit;
  }

  private static boolean containsListGetWithSameIndexAndList(ExpressionTree expression, Symbol listSymbol, ForLoopInitializer initializer) {
    ListGetFinder finder = new ListGetFinder(listSymbol, initializer);
    expression.accept(finder);
    return finder.found && !finder.indexUsedElsewhere && !finder.notLambdaCompatible;
  }

  private static class ListGetFinder extends BaseTreeVisitor {
    private final Symbol listSymbol;
    private final ForLoopInitializer initializer;
    private boolean found = false;
    private boolean indexUsedElsewhere = false;
    private boolean notLambdaCompatible = false;

    ListGetFinder(Symbol listSymbol, ForLoopInitializer initializer) {
      this.listSymbol = listSymbol;
      this.initializer = initializer;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (LIST_GET.matches(tree)) {
        Symbol receiver = extractReceiverSymbol(tree);
        if (listSymbol.equals(receiver) && initializer.hasSameIdentifier(tree.arguments().get(0))) {
          found = true;
          scan(tree.methodSelect());
          return;
        }
      }
      if (throwsCheckedException(tree.methodSymbol())) {
        notLambdaCompatible = true;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (throwsCheckedException(tree.methodSymbol())) {
        notLambdaCompatible = true;
      }
      super.visitNewClass(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (initializer.hasSameIdentifier(tree)) {
        indexUsedElsewhere = true;
      } else if ((tree.symbol().isLocalVariable() || tree.symbol().isParameter())
        && tree.symbol() instanceof Symbol.VariableSymbol variableSymbol
        && !variableSymbol.isEffectivelyFinal()) {
        notLambdaCompatible = true;
      }
      super.visitIdentifier(tree);
    }

    private static boolean throwsCheckedException(Symbol.MethodSymbol symbol) {
      return symbol.thrownTypes().stream()
        .anyMatch(t -> !t.isSubtypeOf("java.lang.RuntimeException") && !t.isSubtypeOf("java.lang.Error"));
    }
  }
}
