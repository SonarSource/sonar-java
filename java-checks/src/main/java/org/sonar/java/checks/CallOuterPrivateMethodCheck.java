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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3398")
public class CallOuterPrivateMethodCheck extends IssuableSubscriptionVisitor {

  private MethodInvocationVisitor methodInvocationVisitor;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    methodInvocationVisitor = new MethodInvocationVisitor();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    methodInvocationVisitor.checkUsages();
    methodInvocationVisitor = null;
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isInnerClass(classSymbol)) {
      methodInvocationVisitor.setClassSymbol(classSymbol);
      classTree.accept(methodInvocationVisitor);
    }
  }

  private static boolean isInnerClass(Symbol symbol) {
    return symbol.owner().isTypeSymbol();
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {
    private final Map<Symbol.TypeSymbol, Map<Symbol.MethodSymbol, Integer>> usagesByInnerClass = new HashMap<>();
    private final Map<String, Set<MethodInvocationTree>> unknownInvocations = new HashMap<>();
    private Symbol.TypeSymbol classSymbol;
    private Map<Symbol.MethodSymbol, Integer> usages;

    public void setClassSymbol(Symbol.TypeSymbol classSymbol) {
      this.classSymbol = classSymbol;
      usages = new HashMap<>();
      usagesByInnerClass.put(classSymbol, usages);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol.MethodSymbol symbol = tree.methodSymbol();
      if (symbol.isUnknown()) {
        String name = ExpressionUtils.methodName(tree).name();
        unknownInvocations.computeIfAbsent(name, k -> new HashSet<>()).add(tree);
      } else if (isPrivateMethodOfOuterClass(symbol) && isInvocationOnCurrentInstance(tree)) {
        if (JUtils.isParametrizedMethod(symbol) && symbol.declaration() != null) {
          // generic methods requires to use their declaration symbol rather than the parameterized one
          symbol = symbol.declaration().symbol();
        }
        usages.compute(symbol, (k, v) -> v == null ? 1 : (v + 1));
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isInvocationOnCurrentInstance(MethodInvocationTree tree) {
      ExpressionTree expressionTree = tree.methodSelect();
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        // Looking for "A.this.f()"
        ExpressionTree memberSelectExpression = ((MemberSelectExpressionTree) expressionTree).expression();
        if (memberSelectExpression.is(Tree.Kind.MEMBER_SELECT)) {
          return ExpressionUtils.isThis(((MemberSelectExpressionTree) memberSelectExpression).identifier());
        }
      }
      return expressionTree.is(Tree.Kind.IDENTIFIER);
    }

    private boolean isPrivateMethodOfOuterClass(Symbol symbol) {
      return symbol.isPrivate() && symbol.owner().equals(classSymbol.owner()) && !"<init>".equals(symbol.name());
    }

    void checkUsages() {

      usagesByInnerClass.forEach((symbol, innerClassUsages) -> innerClassUsages.forEach((methodUsed, count) -> {
        boolean matchArity = unknownInvocations.getOrDefault(methodUsed.name(), new HashSet<>())
          .stream()
          .anyMatch(mit -> hasSameArity(methodUsed, mit));

        // if an unknown method has same name and same arity, do not report, likely a FP.
        if (!matchArity && methodUsed.usages().size() == count) {
          reportIssueOnMethod(methodUsed.declaration(), symbol);
        }
      }));
    }

    private boolean hasSameArity(Symbol.MethodSymbol methodUsed, MethodInvocationTree mit) {
      int formalArity = methodUsed.parameterTypes().size();
      int invokedArity = mit.arguments().size();
      return formalArity == invokedArity ||
        (JUtils.isVarArgsMethod(methodUsed) && invokedArity >= formalArity - 1);
    }

    private void reportIssueOnMethod(@Nullable MethodTree declaration, Symbol.TypeSymbol classSymbol) {
      if (declaration != null && !isIllegalMove(declaration, classSymbol)) {
        String message = "Move this method into ";
        if (classSymbol.name().isEmpty()) {
          message += "the anonymous class declared at line " + ((JavaTree) classSymbol.declaration()).getLine() + ".";
        } else {
          message += "\"" + classSymbol.name() + "\".";
        }
        reportIssue(declaration.simpleName(), message);
      }
    }

    private boolean isIllegalMove(MethodTree declaration, Symbol.TypeSymbol classSymbol) {
      return declaration.symbol().isStatic() && !classSymbol.isStatic();
    }

  }
}
