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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.List;
import java.util.Set;

@Rule(key = "S1168")
public class ReturnEmptyArrayNotNullCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> COLLECTION_TYPES = ImmutableSet.of(
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

  private final Deque<Returns> returnType = Lists.newLinkedList();

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
        } else if (isCollection(returnType)) {
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
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    returnType.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.RETURN_STATEMENT, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      if (isAllowingNull(methodTree)) {
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

  private static boolean isAllowingNull(MethodTree methodTree) {
    for (AnnotationTree annotation : methodTree.modifiers().annotations()) {
      Type type = annotation.annotationType().symbolType();
      if (type.is("javax.annotation.Nullable") || type.is("javax.annotation.CheckForNull")) {
        return true;
      }
    }
    return false;
  }
}
