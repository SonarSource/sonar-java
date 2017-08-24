/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Rule(key = "S4143")
public class OverwrittenKeyCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher MAP_PUT = MethodMatcher.create().typeDefinition("java.util.Map").name("put").parameters(TypeCriteria.anyType(), TypeCriteria.anyType());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    Map<CollectionAndKey, Tree> usedKeys = new HashMap<>();
    for (StatementTree statementTree: ((BlockTree) tree).body()){
      CollectionAndKey mapPut = isMapPut(statementTree);
      if (mapPut != null) {
        handleKey(usedKeys, mapPut);
      } else {
        CollectionAndKey arrayAssignment = isArrayAssignment(statementTree);
        if (arrayAssignment != null) {
          handleKey(usedKeys, arrayAssignment);
        } else {
          // sequence of setting collection values is interrupted
          usedKeys.clear();
        }
      }
    }
  }

  private static class CollectionAndKey {
    private final Symbol collection;
    private final Tree keyTree;
    private final Object key;

    private CollectionAndKey(Symbol collection, Tree keyTree, Object key) {
      this.collection = collection;
      this.keyTree = keyTree;
      this.key = key;
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
      return ((IdentifierTree) collectionExpression).symbol();
    }
    return null;
  }

  @CheckForNull
  private static CollectionAndKey isArrayAssignment(StatementTree statementTree) {
    if (statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statementTree).expression();
      if (expression.is(Tree.Kind.ASSIGNMENT)) {
        ExpressionTree variable = ((AssignmentExpressionTree) expression).variable();
        if (variable.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
          ArrayAccessExpressionTree aaet = (ArrayAccessExpressionTree) variable;
          Symbol collection = symbolFromIdentifier(aaet.expression());
          ExpressionTree keyTree = aaet.dimension().expression();
          Object key = extractKey(keyTree);
          if (collection != null && key != null) {
            return new CollectionAndKey(collection, keyTree, key);
          }
        }
      }
    }
    return null;
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
          return new CollectionAndKey(collection, keyTree, key);
        }
      }
    }
    return null;
  }

  private void handleKey(Map<CollectionAndKey, Tree> keys, CollectionAndKey collectionAndKey) {
    if (keys.containsKey(collectionAndKey)) {
      Tree previousTree = keys.get(collectionAndKey);
      String indexOrKey = collectionAndKey.keyTree.parent().is(Tree.Kind.ARRAY_DIMENSION) ? "index" : "key";
      reportIssue(collectionAndKey.keyTree,
        String.format("Verify this is the %s that was intended; a value has already been saved for it on line %d.", indexOrKey, previousTree.firstToken().line()),
        ImmutableList.of(new JavaFileScannerContext.Location("Original value", previousTree)),
        null);
    } else {
      keys.put(collectionAndKey, collectionAndKey.keyTree);
    }
  }

  @CheckForNull
  private static Object extractKey(ExpressionTree keyArgument) {
    if (keyArgument instanceof LiteralTree) {
      return ((LiteralTree) keyArgument).value();
    }
    return symbolFromIdentifier(keyArgument);
  }
}
