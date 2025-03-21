/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1185")
public class MethodOnlyCallsSuperCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers ALLOWED_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofAnyType().names("toString", "hashCode").addWithoutParametersMatcher().build(),
    MethodMatchers.create().ofAnyType().names("equals").addParametersMatcher("java.lang.Object").build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (ALLOWED_METHODS.matches(methodTree)) {
      return;
    }
    if (isSingleStatementMethod(methodTree)
      && isUselessSuperCall(methodTree)
      && !hasAnnotationDifferentFromOverride(methodTree.modifiers().annotations())
      && !isFinalOrSynchronizedOrStrictFP(methodTree)
      && !isClassAnnotatedWithTransactional(methodTree)) {
      reportIssue(methodTree.simpleName(), "Remove this method to simply inherit it.");
    }
  }

  private static boolean isFinalOrSynchronizedOrStrictFP(MethodTree methodTree) {
    return ModifiersUtils.hasAnyOf(methodTree.modifiers(), Modifier.FINAL, Modifier.SYNCHRONIZED, Modifier.STRICTFP);
  }

  private static boolean isSingleStatementMethod(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    return block != null && block.body().size() == 1;
  }

  private static boolean isUselessSuperCall(MethodTree methodTree) {
    ExpressionTree callToSuper = null;
    StatementTree statementTree = methodTree.block().body().get(0);
    if (returnsVoid(methodTree) && statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      callToSuper = ((ExpressionStatementTree) statementTree).expression();
    } else if (statementTree.is(Tree.Kind.RETURN_STATEMENT)) {
      callToSuper = ((ReturnStatementTree) statementTree).expression();
    }
    if (callToSuper == null || !isCallToSuper(methodTree, callToSuper)) {
      return false;
    }
    Symbol parentMethod = ((MethodInvocationTree) callToSuper).methodSymbol();
    if (parentMethod.isUnknown()) {
      return false;
    }
    return sameVisibility(methodTree.symbol(), parentMethod);
  }

  private static boolean sameVisibility(Symbol.MethodSymbol method, Symbol parentMethod) {
    return bothPackage(method, parentMethod)
      || bothProtected(method, parentMethod)
      || bothPublic(method, parentMethod);
  }

  private static boolean bothPackage(Symbol.MethodSymbol method, Symbol parentMethod) {
    return method.isPackageVisibility() && parentMethod.isPackageVisibility();
  }

  private static boolean bothProtected(Symbol.MethodSymbol method, Symbol parentMethod) {
    return method.isProtected() && parentMethod.isProtected();
  }

  private static boolean bothPublic(Symbol.MethodSymbol method, Symbol parentMethod) {
    return method.isPublic() && parentMethod.isPublic();
  }

  private static boolean isCallToSuper(MethodTree methodTree, Tree callToSuper) {
    if (callToSuper.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) callToSuper;
      if (methodInvocationTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        if (callSuperMethodWithSameName(mset, methodTree) && callsWithSameParameters(methodInvocationTree.arguments(), methodTree.parameters())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean callSuperMethodWithSameName(MemberSelectExpressionTree mset, MethodTree methodTree) {
    return mset.expression().is(Tree.Kind.IDENTIFIER)
      && "super".equals(((IdentifierTree) mset.expression()).name())
      && mset.identifier().name().equals(methodTree.simpleName().name());
  }

  private static boolean callsWithSameParameters(List<ExpressionTree> arguments, List<VariableTree> parameters) {
    if (arguments.size() != parameters.size()) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      ExpressionTree arg = arguments.get(i);
      VariableTree param = parameters.get(i);
      if (!(arg.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) arg).name().equals(param.simpleName().name()))) {
        return false;
      }
    }
    return true;
  }

  private static boolean returnsVoid(MethodTree methodTree) {
    Tree returnType = methodTree.returnType();
    return returnType != null && returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
  }

  private static boolean hasAnnotationDifferentFromOverride(List<AnnotationTree> annotations) {
    for (AnnotationTree annotation : annotations) {
      if (!(annotation.annotationType().is(Tree.Kind.IDENTIFIER) && "Override".equals(((IdentifierTree) annotation.annotationType()).name()))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isClassAnnotatedWithTransactional(MethodTree methodTree) {
    return methodTree.symbol().enclosingClass().metadata().isAnnotatedWith("javax.transaction.Transactional");
  }

}
