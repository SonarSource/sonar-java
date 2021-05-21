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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.collections.SetUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.se.NullableAnnotationUtils.isAnnotatedNullable;

@Rule(key = "S1168")
public class ReturnEmptyArrayNotNullCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> COLLECTION_TYPES = SetUtils.immutableSetOf(
    "Collection",
    "BeanContext",
    "BeanContextServices",
    "BlockingDeque",
    "BlockingQueue",
    "Deque",
    "List",
    "NavigableSet",
    "Queue",
    "Set",
    "SortedSet",
    "AbstractCollection",
    "AbstractList",
    "AbstractQueue",
    "AbstractSequentialList",
    "AbstractSet",
    "ArrayBlockingQueue",
    "ArrayDeque",
    "ArrayList",
    "AttributeList",
    "BeanContextServicesSupport",
    "BeanContextSupport",
    "ConcurrentLinkedQueue",
    "ConcurrentSkipListSet",
    "CopyOnWriteArrayList",
    "CopyOnWriteArraySet",
    "DelayQueue",
    "EnumSet",
    "HashSet",
    "JobStateReasons",
    "LinkedBlockingDeque",
    "LinkedBlockingQueue",
    "LinkedHashSet",
    "LinkedList",
    "PriorityBlockingQueue",
    "PriorityQueue",
    "RoleList",
    "RoleUnresolvedList",
    "Stack",
    "SynchronousQueue",
    "TreeSet",
    "Vector");

  private static final String ITEM_PROCESSOR_PROCESS = "process";
  private static final MethodMatchers ITEM_PROCESSOR_PROCESS_METHOD = MethodMatchers.create()
    .ofSubTypes("org.springframework.batch.item.ItemProcessor").names(ITEM_PROCESSOR_PROCESS).withAnyParameters().build();

  private final Deque<Returns> returnType = new LinkedList<>();

  private enum Returns {
    ARRAY, COLLECTION, OTHERS;

    public static Returns getReturnType(@Nullable Tree tree) {
      if (tree != null) {
        Tree returnType = tree;
        while (returnType.is(Tree.Kind.PARAMETERIZED_TYPE)) {
          returnType = ((ParameterizedTypeTree) returnType).type();
        }
        if (returnType.is(Tree.Kind.ARRAY_TYPE)) {
          return ARRAY;
        }
        if (isCollection(returnType)) {
          return COLLECTION;
        }
      }
      return OTHERS;
    }

    private static boolean isCollection(Tree methodReturnType) {
      IdentifierTree identifierTree = null;
      if (methodReturnType.is(Tree.Kind.IDENTIFIER)) {
        identifierTree = (IdentifierTree) methodReturnType;
      } else if (methodReturnType.is(Tree.Kind.MEMBER_SELECT)) {
        identifierTree = ((MemberSelectExpressionTree) methodReturnType).identifier();
      }
      return identifierTree != null && COLLECTION_TYPES.contains(identifierTree.name());
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    returnType.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.RETURN_STATEMENT, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      SymbolMetadata metadata = methodTree.symbol().metadata();
      if (hasUnknownAnnotation(metadata) || isAnnotatedNullable(metadata) || requiresReturnNull(methodTree)) {
        returnType.push(Returns.OTHERS);
      } else {
        returnType.push(Returns.getReturnType(methodTree.returnType()));
      }
    } else if (tree.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.LAMBDA_EXPRESSION)) {
      returnType.push(Returns.OTHERS);
    } else {
      ReturnStatementTree returnStatement = (ReturnStatementTree) tree;
      if (isReturningNull(returnStatement)) {
        if (returnType.peek().equals(Returns.ARRAY)) {
          reportIssue(returnStatement.expression(), "Return an empty array instead of null.");
        } else if (returnType.peek().equals(Returns.COLLECTION)) {
          reportIssue(returnStatement.expression(), "Return an empty collection instead of null.");
        }
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!tree.is(Tree.Kind.RETURN_STATEMENT)) {
      returnType.pop();
    }
  }

  private static boolean isReturningNull(ReturnStatementTree tree) {
    ExpressionTree expression = tree.expression();
    return expression != null && expression.is(Tree.Kind.NULL_LITERAL);
  }

  private static boolean requiresReturnNull(MethodTree methodTree) {
    if (!ITEM_PROCESSOR_PROCESS.equals(methodTree.simpleName().name())) {
      return false;
    }

    Symbol owner = methodTree.symbol().owner();
    if (owner == null || !owner.isTypeSymbol()) {
      // Unknown hierarchy, consider it as requires null to avoid FP
      // At this point, owner should never be null, defensive programming
      return true;
    }
    List<Type> interfaces = ((Symbol.TypeSymbol) owner).interfaces();
    Boolean isOverriding = methodTree.isOverriding();
    if (isOverriding == null || interfaces.stream().anyMatch(Type::isUnknown)) {
      // Return true in case of incomplete semantic
      return true;
    }
    return Boolean.TRUE.equals(isOverriding) && ITEM_PROCESSOR_PROCESS_METHOD.matches(methodTree);
  }

  private static boolean hasUnknownAnnotation(SymbolMetadata symbolMetadata) {
    return symbolMetadata.annotations().stream().anyMatch(annotation -> annotation.symbol().isUnknown());
  }
}
