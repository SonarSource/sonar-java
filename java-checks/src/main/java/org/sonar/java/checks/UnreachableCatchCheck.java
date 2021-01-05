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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    new UnreachableCatchFinder(tryStatementTree).find();
  }

  private class UnreachableCatchFinder {
    Map<Type, Tree> typeToTypeTree = new HashMap<>();
    List<CatchClauseInfo> catchClauses = new ArrayList<>();
    Map<Type, Set<Type>> baseToSubtype = new HashMap<>();
    TryStatementTree tryStatementTree;
    List<Type> thrownTypes;

    UnreachableCatchFinder(TryStatementTree tryStatementTree) {
      this.tryStatementTree = tryStatementTree;
    }

    void find() {
      getBaseTypeCaughtAfterSubtype(tryStatementTree.catches());
      if (baseToSubtype.isEmpty()) {
        return;
      }

      ThrownExceptionCollector collector = new ThrownExceptionCollector();
      tryStatementTree.block().accept(collector);

      if (collector.unknownVisited || collector.thrownTypes.isEmpty()) {
        // Unknown method can throw anything, we can not tell anything about it.
        return;
      }

      thrownTypes = collector.thrownTypes;

      for (CatchClauseInfo catchClause : catchClauses) {
        List<Type> hiddenTypes = catchClause.types.stream()
          .filter(type -> isUnreachable(type, baseToSubtype.getOrDefault(type, Collections.emptySet()), thrownTypes))
          .collect(Collectors.toList());

        if (hiddenTypes.size() == catchClause.types.size()) {
          reportWholeCatchClause(catchClause);
        } else {
          for (Type hiddenType : hiddenTypes) {
            reportSingleType(hiddenType);
          }
        }
      }
    }

    void reportWholeCatchClause(CatchClauseInfo catchClause) {
      List<Type> subtypesHiding = catchClause.types.stream()
        .flatMap(type -> baseToSubtype.get(type).stream())
        .filter(subtype -> isHiding(subtype, thrownTypes))
        .collect(Collectors.toList());

      reportIssue(catchClause.tree.catchKeyword(),
        "Remove or refactor this catch clause because it is unreachable as hidden by previous catch blocks.",
        subtypesHiding.stream().map(type -> new JavaFileScannerContext.Location("Already catch the exception", typeToTypeTree.get(type)))
          .collect(Collectors.toList()),
        null);
    }

    void reportSingleType(Type hiddenType) {
      List<Type> subtypesHiding = baseToSubtype.get(hiddenType).stream()
        .filter(subtype -> isHiding(subtype, thrownTypes))
        .collect(Collectors.toList());

      reportIssue(typeToTypeTree.get(hiddenType),
        "Remove this type because it is unreachable as hidden by previous catch blocks.",
        subtypesHiding.stream().map(type -> new JavaFileScannerContext.Location("Already catch the exception", typeToTypeTree.get(type)))
          .collect(Collectors.toList()),
        null);
    }

    void getBaseTypeCaughtAfterSubtype(List<CatchTree> catches) {
      List<Type> catchTypes = catches.stream()
        .flatMap(c -> {
          List<Type> types = new ArrayList<>();
          collectTypesFromTypeTree(c.parameter().type(), types);
          catchClauses.add(new CatchClauseInfo(types, c));
          return types.stream();
        })
        .filter(UnreachableCatchCheck::isChecked)
        .collect(Collectors.toList());

      for (int i = 0; i < catchTypes.size() - 1; i++) {
        Type topType = catchTypes.get(i);
        for (int j = i + 1; j < catchTypes.size(); j++) {
          Type bottomType = catchTypes.get(j);
          if (topType.isSubtypeOf(bottomType)) {
            baseToSubtype.computeIfAbsent(bottomType, k -> new HashSet<>()).add(topType);
          }
        }
      }
    }

    void collectTypesFromTypeTree(TypeTree typeTree, List<Type> types) {
      if (typeTree.is(Tree.Kind.UNION_TYPE)) {
        ((UnionTypeTree) typeTree).typeAlternatives().forEach(t -> collectTypesFromTypeTree(t, types));
      } else {
        Type type = typeTree.symbolType();
        typeToTypeTree.put(type, typeTree);
        types.add(type);
      }
    }

  }

  private static boolean isChecked(Type type) {
    return !type.isUnknown()
      && !type.isSubtypeOf("java.lang.RuntimeException")
      && !type.isSubtypeOf("java.lang.Error")
      && !type.is("java.lang.Exception")
      && !type.is("java.lang.Throwable");
  }

  private static boolean isHiding(Type subtype, List<Type> thrownTypes) {
    return thrownTypes.stream()
      .anyMatch(thrownType ->
        thrownType.isSubtypeOf(subtype)
      );
  }

  private static boolean isUnreachable(Type baseType, Collection<Type> subtypes, List<Type> thrownTypes) {
    return !subtypes.isEmpty() && thrownTypes.stream()
      .allMatch(thrownType ->
        !relatedTypes(thrownType, baseType) || subtypes.stream().anyMatch(thrownType::isSubtypeOf)
      );
  }

  private static boolean relatedTypes(Type type1, Type type2) {
    return type1.isSubtypeOf(type2) || type2.isSubtypeOf(type1);
  }

  private static class ThrownExceptionCollector extends BaseTreeVisitor {
    List<Type> thrownTypes = new ArrayList<>();
    boolean unknownVisited = false;

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
        List<Type> newThrownTypes = ((Symbol.MethodSymbol) symbol).thrownTypes();
        thrownTypes.addAll(newThrownTypes);
        unknownVisited = unknownVisited || newThrownTypes.stream().anyMatch(Type::isUnknown);
      } else if (symbol.isUnknown()) {
        unknownVisited = true;
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

  private static class CatchClauseInfo {

    List<Type> types;
    CatchTree tree;

    CatchClauseInfo(List<Type> types, CatchTree tree) {
      this.types = types;
      this.tree = tree;
    }

  }

}
