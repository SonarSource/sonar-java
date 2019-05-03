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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2250")
public class CollectionMethodsWithLinearComplexityCheck extends IssuableSubscriptionVisitor {

  private static final String ARRAY_LIST = "java.util.ArrayList";
  private static final String LINKED_LIST = "java.util.LinkedList";
  private static final String COPY_ON_WRITE_ARRAY_LIST = "java.util.concurrent.CopyOnWriteArrayList";
  private static final String COPY_ON_WRITE_ARRAY_SET = "java.util.concurrent.CopyOnWriteArraySet";
  private static final String CONCURRENT_LINKED_QUEUE = "java.util.concurrent.ConcurrentLinkedQueue";
  private static final String CONCURRENT_LINKED_DEQUE = "java.util.concurrent.ConcurrentLinkedDeque";

  private static MethodMatcher collectionMethodMatcher() {
    return MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.Collection"));
  }

  private static final Map<MethodMatcher, Set<String>> matcherActualTypeMap;
  static {
    ImmutableMap.Builder<MethodMatcher, Set<String>> builder = ImmutableMap.builder();

    MethodMatcher collectionContains = collectionMethodMatcher().name("contains").addParameter("java.lang.Object");
    builder.put(collectionContains, ImmutableSet.of(ARRAY_LIST, LINKED_LIST, COPY_ON_WRITE_ARRAY_LIST, COPY_ON_WRITE_ARRAY_SET, CONCURRENT_LINKED_QUEUE, CONCURRENT_LINKED_DEQUE));

    MethodMatcher collectionSize = collectionMethodMatcher().name("size").withoutParameter();
    builder.put(collectionSize, ImmutableSet.of(CONCURRENT_LINKED_QUEUE, CONCURRENT_LINKED_DEQUE));

    MethodMatcher collectionAdd = collectionMethodMatcher().name("add").addParameter(TypeCriteria.anyType());
    builder.put(collectionAdd, ImmutableSet.of(COPY_ON_WRITE_ARRAY_SET, COPY_ON_WRITE_ARRAY_LIST));

    MethodMatcher collectionRemove = collectionMethodMatcher().name("remove").addParameter("java.lang.Object");
    builder.put(collectionRemove, ImmutableSet.of(ARRAY_LIST, COPY_ON_WRITE_ARRAY_SET, COPY_ON_WRITE_ARRAY_LIST));

    MethodMatcher listGet = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.List")).name("get").addParameter("int");
    builder.put(listGet, Collections.singleton(LINKED_LIST));
    matcherActualTypeMap = builder.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    matcherActualTypeMap.forEach((methodMatcher, actualTypes) -> {
      if (methodMatcher.matches(mit) && invocationInMethod(mit)) {
        Symbol target = invocationTarget(mit);
        if (target != null && isField(target) && matchesActualType(target, actualTypes)) {
          IdentifierTree methodName = ExpressionUtils.methodName(mit);
          reportIssue(methodName, "This call to \"" + methodName.name() + "()\" may be a performance hot spot if the collection is large.");
        }
      }
    });
  }

  private static boolean invocationInMethod(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD)) {
      parent = parent.parent();
    }
    return parent != null;
  }

  private static boolean isField(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && !"this".equals(symbol.name()) && !"super".equals(symbol.name());
  }

  private static boolean matchesActualType(Symbol invocationTarget, Set<String> actualTypes) {
    Type declaredType = invocationTarget.type();
    if (actualTypes.contains(declaredType.fullyQualifiedName())) {
      return true;
    }
    // actual type is looked up only on private or final fields, otherwise it can't be guaranteed
    if (invocationTarget.isPrivate() || invocationTarget.isFinal()) {
      Set<String> assignedTypes = findAssignedTypes(invocationTarget);
      return !assignedTypes.isEmpty() && actualTypes.containsAll(assignedTypes);
    }
    return false;
  }

  @CheckForNull
  private static Symbol invocationTarget(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree methodSelectExpression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (methodSelectExpression.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) methodSelectExpression).symbol();
      }
    }
    return null;
  }

  private static Set<String> findAssignedTypes(Symbol symbol) {
    Set<String> types = new HashSet<>();
    Tree declaration = symbol.declaration();
    if (declaration != null && declaration.is(Tree.Kind.VARIABLE)) {
      ExpressionTree initializer = ((VariableTree) declaration).initializer();
      if (initializer != null) {
        types.add(initializer.symbolType().fullyQualifiedName());
      }
    }
    symbol.usages().stream()
      .flatMap(CollectionMethodsWithLinearComplexityCheck::usageInAssignment)
      .map(assignment -> assignment.expression().symbolType().fullyQualifiedName())
      .forEach(types::add);
    return types;
  }

  private static Stream<AssignmentExpressionTree> usageInAssignment(IdentifierTree usage) {
    Tree prevParent = usage;
    Tree parent = usage.parent();
    while (parent != null && !parent.is(Tree.Kind.ASSIGNMENT) && parent.is(Tree.Kind.MEMBER_SELECT, Tree.Kind.IDENTIFIER)) {
      prevParent = parent;
      parent = parent.parent();
    }
    if (parent != null && parent.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) parent;
      if (assignment.variable().equals(prevParent)) {
        return Stream.of(assignment);
      }
    }
    return Stream.empty();
  }
}
