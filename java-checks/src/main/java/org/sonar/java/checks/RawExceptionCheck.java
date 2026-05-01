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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00112", repositoryKey = "squid")
@Rule(key = "S112")
public class RawExceptionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final List<String> RAW_EXCEPTIONS = Arrays.asList(
    "java.lang.Throwable",
    "java.lang.Error",
    "java.lang.Exception",
    "java.lang.RuntimeException");

  private FluentReporting context;
  private JavaVersion javaVersion;
  private final Set<Type> exceptionsThrownByMethodInvocations = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = (FluentReporting) context;
    this.javaVersion = context.getJavaVersion();
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree method) {
    super.visitMethod(method);
    if ((method.is(Tree.Kind.CONSTRUCTOR) || isNotOverridden(method)) && isNotMainMethod(method) && hasNoUnknownMethod(method)) {
      for (TypeTree throwClause : method.throwsClauses()) {
        Type exceptionType = throwClause.symbolType();
        if (isRawException(exceptionType) && !exceptionsThrownByMethodInvocations.contains(exceptionType)) {
          reportIssue(throwClause);
        }
      }
    }
    exceptionsThrownByMethodInvocations.clear();
  }

  private static boolean hasNoUnknownMethod(MethodTree method) {
    MethodTreeUtils.MethodInvocationCollector unknownMethodVisitor = new MethodTreeUtils.MethodInvocationCollector(Symbol::isUnknown);
    method.accept(unknownMethodVisitor);
    return unknownMethodVisitor.getInvocationTree().isEmpty();
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    if (tree.expression().is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) tree.expression();
      TypeTree exception = newClassTree.identifier();
      if (isRawException(exception.symbolType()) && !isSpecificCheckedExceptionWrapper(newClassTree)) {
        reportIssue(exception);
      }
    }
    super.visitThrowStatement(tree);
  }

  private static boolean isSpecificCheckedExceptionWrapper(NewClassTree newClassTree) {
    if (!isAllowedWrapperType(newClassTree.identifier().symbolType())) {
      return false;
    }
    Tree catchTree = ExpressionUtils.getParentOfType(newClassTree, Tree.Kind.CATCH);
    if (!(catchTree instanceof CatchTree enclosingCatch)) {
      return false;
    }
    VariableTree catchParameter = enclosingCatch.parameter();
    return caughtTypes(catchParameter).stream().allMatch(RawExceptionCheck::isSpecificCheckedException) &&
      newClassTree.arguments().stream().anyMatch(argument -> isCaughtExceptionCause(argument, catchParameter));
  }

  private static boolean isAllowedWrapperType(Type type) {
    return type.is("java.lang.RuntimeException") || type.is("java.lang.Error");
  }

  private static List<TypeTree> caughtTypes(VariableTree catchParameter) {
    TypeTree type = catchParameter.type();
    if (type.is(Tree.Kind.UNION_TYPE)) {
      return ((UnionTypeTree) type).typeAlternatives();
    }
    return Collections.singletonList(type);
  }

  private static boolean isSpecificCheckedException(TypeTree typeTree) {
    Type type = typeTree.symbolType();
    return isSpecificCheckedException(type) ||
      (!type.isUnknown() && !type.isSubtypeOf("java.lang.Throwable") && isSpecificExceptionTypeName(typeTree));
  }

  private static boolean isSpecificCheckedException(Type type) {
    return type.isUnknown() || (type.isSubtypeOf("java.lang.Throwable") &&
      !isRawException(type) &&
      !type.isSubtypeOf("java.lang.RuntimeException") &&
      !type.isSubtypeOf("java.lang.Error"));
  }

  private static boolean isSpecificExceptionTypeName(TypeTree typeTree) {
    String typeName = simpleTypeName(typeTree);
    return typeName != null &&
      !"Throwable".equals(typeName) &&
      !"Error".equals(typeName) &&
      !"Exception".equals(typeName) &&
      !"RuntimeException".equals(typeName) &&
      !typeName.endsWith("RuntimeException") &&
      !typeName.endsWith("Error");
  }

  private static String simpleTypeName(TypeTree typeTree) {
    return switch (typeTree.kind()) {
      case IDENTIFIER -> ((IdentifierTree) typeTree).name();
      case MEMBER_SELECT -> ((MemberSelectExpressionTree) typeTree).identifier().name();
      case PARAMETERIZED_TYPE -> simpleTypeName(((ParameterizedTypeTree) typeTree).type());
      default -> null;
    };
  }

  private static boolean isCaughtExceptionCause(ExpressionTree argument, VariableTree catchParameter) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(argument);
    return isIdentifierForCatchParameter(expression, catchParameter) ||
      isCauseMethodInvocation(expression, catchParameter) ||
      isLocalVariableInitializedWithCause(expression, catchParameter);
  }

  private static boolean isLocalVariableInitializedWithCause(ExpressionTree expression, VariableTree catchParameter) {
    if (!(expression instanceof IdentifierTree identifier) ||
      !(identifier.symbol() instanceof Symbol.VariableSymbol variableSymbol) ||
      !variableSymbol.isLocalVariable() ||
      !variableSymbol.isEffectivelyFinal()) {
      return false;
    }
    VariableTree declaration = variableSymbol.declaration();
    ExpressionTree initializer = declaration == null ? null : declaration.initializer();
    return initializer != null && isCauseMethodInvocation(initializer, catchParameter);
  }

  private static boolean isCauseMethodInvocation(ExpressionTree expression, VariableTree catchParameter) {
    ExpressionTree normalizedExpression = ExpressionUtils.skipParentheses(expression);
    if (!(normalizedExpression instanceof MethodInvocationTree methodInvocationTree) ||
      !methodInvocationTree.arguments().isEmpty() ||
      !(methodInvocationTree.methodSelect() instanceof MemberSelectExpressionTree memberSelect)) {
      return false;
    }
    String methodName = memberSelect.identifier().name();
    return ("getCause".equals(methodName) || "getTargetException".equals(methodName)) &&
      isIdentifierForCatchParameter(memberSelect.expression(), catchParameter);
  }

  private static boolean isIdentifierForCatchParameter(ExpressionTree expression, VariableTree catchParameter) {
    ExpressionTree normalizedExpression = ExpressionUtils.skipParentheses(expression);
    if (!(normalizedExpression instanceof IdentifierTree identifier)) {
      return false;
    }
    Symbol symbol = catchParameter.symbol();
    Symbol identifierSymbol = identifier.symbol();
    if (!symbol.isUnknown() && !identifierSymbol.isUnknown()) {
      return identifierSymbol.equals(symbol);
    }
    return identifier.name().equals(catchParameter.simpleName().name());
  }

  private void reportIssue(Tree tree) {
    context.newIssue()
      .forRule(this)
      .onTree(tree)
      .withMessage("Replace generic exceptions with specific library exceptions or a custom exception.")
      .report();
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    collectThrownTypes(mit.methodSymbol());
    super.visitMethodInvocation(mit);
  }

  @Override
  public void visitNewClass(NewClassTree nct) {
    collectThrownTypes(nct.methodSymbol());
    super.visitNewClass(nct);
  }

  private void collectThrownTypes(Symbol.MethodSymbol symbol) {
    if (!symbol.isUnknown()) {
      exceptionsThrownByMethodInvocations.addAll(symbol.thrownTypes());
    }
  }

  private static boolean isRawException(Type type) {
    return RAW_EXCEPTIONS.stream().anyMatch(type::is);
  }

  private static boolean isNotOverridden(MethodTree tree) {
    return Boolean.FALSE.equals(tree.isOverriding());
  }

  private boolean isNotMainMethod(MethodTree tree) {
    return !MethodTreeUtils.isMainMethod(tree, javaVersion);
  }

}
