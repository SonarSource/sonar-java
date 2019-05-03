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

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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

import java.util.Collections;
import java.util.List;

@Rule(key = "S1185")
public class MethodOnlyCallsSuperCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final MethodMatcherCollection ALLOWED_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create().name("toString").typeDefinition(TypeCriteria.anyType()).withoutParameter(),
    MethodMatcher.create().name("hashCode").typeDefinition(TypeCriteria.anyType()).withoutParameter(),
    MethodMatcher.create().name("equals").typeDefinition(TypeCriteria.anyType()).parameters(JAVA_LANG_OBJECT));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (ALLOWED_METHODS.anyMatch(methodTree)) {
      return;
    }
    if (isSingleStatementMethod(methodTree) && isUselessSuperCall(methodTree)
      && !hasAnnotationDifferentFromOverride(methodTree.modifiers().annotations()) && !isFinal(methodTree)) {
      reportIssue(methodTree.simpleName(), "Remove this method to simply inherit it.");
    }
  }

  private static boolean isFinal(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.FINAL);
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
    return callToSuper != null && isCallToSuper(methodTree, callToSuper) && sameVisibility(methodTree.symbol(), ((MethodInvocationTree) callToSuper).symbol());
  }

  private static boolean sameVisibility(Symbol.MethodSymbol method, Symbol parentMethod) {
    if (parentMethod.isUnknown()) {
      return true;
    }
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

}
