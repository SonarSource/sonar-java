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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

@Rule(key = "S4970")
public class UnreachableCatchCheck extends IssuableSubscriptionVisitor {

  private Map<Type, SyntaxToken> typeToCatchToken = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatementTree = (TryStatementTree) tree;
    if (!tryStatementTree.resourceList().isEmpty()) {
      // Try with resource will call close, often throwing an exception.
      return;
    }
    typeToCatchToken.clear();
    Multimap<Type, Type> baseToDerived = getBaseTypeCaughtAfterDerivedType(tryStatementTree.catches());

    if (baseToDerived.isEmpty()) {
      return;
    }

    ThrownExceptionCollector collector = new ThrownExceptionCollector();
    tryStatementTree.block().accept(collector);
    List<Type> thrownTypes = collector.thrownTypes;

    baseToDerived.asMap().forEach((baseType, derivedTypes) -> {
      // Catching a derived type before the base type is fine if the body of the try throws an exception which is a subtype of the base type,
      // but not of the derived type. We have to make sure that we are not in this situation before reporting an issue.
      List<Type> derivedTypesHiding = derivedTypes.stream()
        .filter(derivedType -> isHidden(baseType, derivedType, thrownTypes))
        .collect(Collectors.toList());

      if (!derivedTypesHiding.isEmpty()) {
        reportIssue(typeToCatchToken.get(baseType),
          "Remove this catch block because it is unreachable as hidden by previous catch blocks.",
          derivedTypesHiding.stream().map(type -> new JavaFileScannerContext.Location("Already catch the exception", typeToCatchToken.get(type)))
          .collect(Collectors.toList()),
          null);
      }
    });
  }

  private Multimap<Type, Type> getBaseTypeCaughtAfterDerivedType(List<CatchTree> catches) {
    Multimap<Type, Type> baseAfterDerived = HashMultimap.create();
    List<Type> catchTypes = catches.stream()
      .flatMap(c -> {
        List<Type> types = new ArrayList<>();
        collectTypesFromTypeTree(c.parameter().type(), types, c.catchKeyword());
        return types.stream();
      })
      .filter(UnreachableCatchCheck::isChecked)
      .collect(Collectors.toList());

    for (int i = 0; i < catchTypes.size() - 1; i++) {
      Type topType = catchTypes.get(i);
      for (int j = i + 1; j < catchTypes.size(); j++) {
        Type bottomType = catchTypes.get(j);
        if (topType.isSubtypeOf(bottomType)) {
          baseAfterDerived.put(bottomType, topType);
        }
      }
    }
    return baseAfterDerived;
  }

  private void collectTypesFromTypeTree(TypeTree typeTree, List<Type> types, SyntaxToken correspondingCatch) {
    if (typeTree.is(Tree.Kind.UNION_TYPE)) {
      ((UnionTypeTree) typeTree).typeAlternatives().forEach(t -> collectTypesFromTypeTree(t, types, correspondingCatch));
    } else {
      Type type = typeTree.symbolType();
      typeToCatchToken.put(type, correspondingCatch);
      types.add(type);
    }
  }

  private static boolean isChecked(Type type) {
    return !type.isSubtypeOf("java.lang.RuntimeException")
      && !type.isSubtypeOf("java.lang.Error")
      && !type.is("java.lang.Exception")
      && !type.is("java.lang.Throwable");
  }

  private static boolean isHidden(Type baseType, Type derivedType, List<Type> thrownTypes) {
    return thrownTypes.stream().noneMatch(thrownType ->
      thrownType.isSubtypeOf(baseType) && !thrownType.isSubtypeOf(derivedType)
    );
  }

  private static class ThrownExceptionCollector extends BaseTreeVisitor {
    List<Type> thrownTypes = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      addAllThrownTypes(mit.symbol());
      super.visitMethodInvocation(mit);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      addAllThrownTypes(tree.constructorSymbol());
      super.visitNewClass(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      thrownTypes.add(tree.expression().symbolType());
      super.visitThrowStatement(tree);
    }

    private void addAllThrownTypes(Symbol symbol) {
      if (symbol.isMethodSymbol()) {
        thrownTypes.addAll(((Symbol.MethodSymbol) symbol).thrownTypes());
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Skip class
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // Skip lambdas
    }
  }

}
