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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Rule(key = "S4143")
public class OverwrittenKeyCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher MAP_PUT = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.Map")).name("put")
    .parameters(TypeCriteria.anyType(), TypeCriteria.anyType());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ListMultimap<CollectionAndKey, Tree> usedKeys = ArrayListMultimap.create();
    for (StatementTree statementTree: ((BlockTree) tree).body()){
      CollectionAndKey mapPut = isMapPut(statementTree);
      if (mapPut != null) {
        usedKeys.put(mapPut, mapPut.keyTree);
      } else {
        CollectionAndKey arrayAssignment = isArrayAssignment(statementTree);
        if (arrayAssignment != null) {
          if (arrayAssignment.collectionOnRHS()) {
            usedKeys.clear();
          }
          usedKeys.put(arrayAssignment, arrayAssignment.keyTree);
        } else {
          // sequence of setting collection values is interrupted
          reportOverwrittenKeys(usedKeys);
          usedKeys.clear();
        }
      }
    }
    reportOverwrittenKeys(usedKeys);
  }

  private void reportOverwrittenKeys(ListMultimap<CollectionAndKey, Tree> usedKeys) {
    Multimaps.asMap(usedKeys).forEach( (key, trees) -> {
      if (trees.size() > 1) {
        Tree firstUse = trees.get(0);
        Tree firstOverwrite = trees.get(1);
        List<Tree> rest = trees.subList(2, trees.size());
        reportIssue(firstOverwrite,"Verify this is the " + key.indexOrKey() + " that was intended; it was already set before.", secondaryLocations(key, firstUse, rest), 0);
      }
    });
  }

  private static List<JavaFileScannerContext.Location> secondaryLocations(CollectionAndKey key, Tree firstUse, List<Tree> rest) {
    return Stream.concat(
      Stream.of(new JavaFileScannerContext.Location("Original value", firstUse)),
      rest.stream().map(t -> new JavaFileScannerContext.Location("Same " + key.indexOrKey() + " is set", t)))
      .collect(Collectors.toList());
  }

  private static class CollectionAndKey {
    private final Symbol collection;
    private final Tree keyTree;
    private final Object key;
    private final boolean isArray;
    private ExpressionTree rhs;

    private CollectionAndKey(Symbol collection, Tree keyTree, Object key, boolean isArray, ExpressionTree expression) {
      this.collection = collection;
      this.keyTree = keyTree;
      this.key = key;
      this.isArray = isArray;
      this.rhs = expression;
    }

    private boolean collectionOnRHS() {
      FindSymbolUsage findSymbolUsage = new FindSymbolUsage(collection);
      rhs.accept(findSymbolUsage);
      return findSymbolUsage.used;
    }

    private String indexOrKey() {
      return isArray ? "index" : "key";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CollectionAndKey that = (CollectionAndKey) o;
      return Objects.equals(collection, that.collection) &&
        Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(collection, key);
    }
  }

  @CheckForNull
  private static Symbol symbolFromIdentifier(ExpressionTree collectionExpression) {
    if (collectionExpression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) collectionExpression).symbol();
      if (!symbol.isUnknown()) {
        return symbol;
      }
    }
    return null;
  }

  @CheckForNull
  private static CollectionAndKey isArrayAssignment(StatementTree statementTree) {
    if (statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statementTree).expression();
      if (expression.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
        ExpressionTree variable = assignment.variable();
        if (variable.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
          ArrayAccessExpressionTree aaet = (ArrayAccessExpressionTree) variable;
          Symbol collection = symbolFromIdentifier(aaet.expression());
          ExpressionTree keyTree = aaet.dimension().expression();
          Object key = extractKey(keyTree);
          if (collection != null && key != null) {
            return new CollectionAndKey(collection, keyTree, key, true, assignment.expression());
          }
        }
      }
    }
    return null;
  }

  private static class FindSymbolUsage extends BaseTreeVisitor {

    private final Symbol symbol;
    private boolean used;

    public FindSymbolUsage(Symbol symbol) {
      this.symbol = symbol;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (!used) {
        used = tree.symbol() == symbol;
      }
    }
  }


  @CheckForNull
  private static CollectionAndKey isMapPut(StatementTree statementTree) {
    if (statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statementTree).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION) && MAP_PUT.matches((MethodInvocationTree) expression)) {
        MethodInvocationTree mapPut = (MethodInvocationTree) expression;
        Symbol collection = mapPut.methodSelect().is(Tree.Kind.MEMBER_SELECT) ? symbolFromIdentifier(((MemberSelectExpressionTree) mapPut.methodSelect()).expression()) : null;
        ExpressionTree keyTree = mapPut.arguments().get(0);
        Object key = extractKey(keyTree);
        if (collection != null && key != null) {
          return new CollectionAndKey(collection, keyTree, key, false, null);
        }
      }
    }
    return null;
  }


  @CheckForNull
  private static Object extractKey(ExpressionTree keyArgument) {
    if (keyArgument instanceof LiteralTree) {
      return ((LiteralTree) keyArgument).value();
    }
    return symbolFromIdentifier(keyArgument);
  }
}
