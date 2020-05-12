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
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

@Rule(key = "S4970")
public class UnreachableCatchCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatementTree = (TryStatementTree) tree;
    if (!tryStatementTree.resourceList().isEmpty()) {
      // Try with resource will call close, throwing by default an Exception or IOException.
      // Supporting potential problems is not worth since it is really unlikely that something wrong happen.
      return;
    }
    Map<Type, Tree> typeToTypeTree = new HashMap<>();
    Multimap<Type, Type> baseToDerived = getBaseTypeCaughtAfterDerivedType(tryStatementTree.catches(), typeToTypeTree);

    if (baseToDerived.isEmpty()) {
      return;
    }

    ThrownExceptionCollector collector = new ThrownExceptionCollector();
    tryStatementTree.block().accept(collector);
    List<Type> thrownTypes = collector.thrownTypes;

    baseToDerived.asMap().forEach((baseType, derivedTypes) -> {
      List<Type> derivedTypesHiding = derivedTypes.stream()
        .filter(derivedType -> isHiding(derivedType, thrownTypes))
        .collect(Collectors.toList());

      if (!derivedTypesHiding.isEmpty()) {
        reportIssue(typeToTypeTree.get(baseType),
          "Remove this type because it is unreachable as hidden by previous catch blocks.",
          derivedTypesHiding.stream().map(type -> new JavaFileScannerContext.Location("Already catch the exception", typeToTypeTree.get(type)))
          .collect(Collectors.toList()),
          null);
      }
    });
  }

  private static Multimap<Type, Type> getBaseTypeCaughtAfterDerivedType(List<CatchTree> catches, Map<Type, Tree> typeToTypeTree) {
    Multimap<Type, Type> baseAfterDerived = HashMultimap.create();
    List<Type> catchTypes = catches.stream()
      .flatMap(c -> {
        List<Type> types = new ArrayList<>();
        collectTypesFromTypeTree(c.parameter().type(), types, typeToTypeTree);
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

  private static void collectTypesFromTypeTree(TypeTree typeTree, List<Type> types, Map<Type, Tree> typeToTypeTree) {
    if (typeTree.is(Tree.Kind.UNION_TYPE)) {
      ((UnionTypeTree) typeTree).typeAlternatives().forEach(t -> collectTypesFromTypeTree(t, types, typeToTypeTree));
    } else {
      Type type = typeTree.symbolType();
      typeToTypeTree.put(type, typeTree);
      types.add(type);
    }
  }

  private static boolean isChecked(Type type) {
    return !type.isUnknown()
      && !type.isSubtypeOf("java.lang.RuntimeException")
      && !type.isSubtypeOf("java.lang.Error")
      && !type.is("java.lang.Exception")
      && !type.is("java.lang.Throwable");
  }

  private static boolean isHiding(Type derivedType, List<Type> thrownTypes) {
    return thrownTypes.stream().allMatch(thrownType ->
      // Only throwing a subtype of the first caught exception, hiding the base one
      thrownType.isSubtypeOf(derivedType) ||
      // Or throwing an unrelated exception
      (!thrownType.isUnknown() && !derivedType.isUnknown() && !derivedType.isSubtypeOf(thrownType))
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
