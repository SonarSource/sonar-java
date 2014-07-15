/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S1168",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ReturnEmptyArrayyNotNullCheck extends SquidCheck<LexerlessGrammar> {

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

  @Override
  public void init() {
    subscribeTo(JavaGrammar.RETURN_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isReturningNull(node)) {
      AstNode method = getMethod(node);

      if (isReturningArray(method)) {
        getContext().createLineViolation(this, "Return an empty array instead of null.", node);
      } else if (isReturningCollection(method)) {
        getContext().createLineViolation(this, "Return an empty collection instead of null.", node);
      }
    }
  }

  private static boolean isReturningNull(AstNode node) {
    AstNode expression = node.getFirstChild(JavaGrammar.EXPRESSION);

    return expression != null &&
      expression.getToken().equals(expression.getLastToken()) &&
      "null".equals(expression.getTokenOriginalValue());
  }

  private static AstNode getMethod(AstNode node) {
    return node.getFirstAncestor(JavaGrammar.METHOD_DECLARATOR_REST,
        JavaGrammar.CLASS_BODY_DECLARATION,
        JavaGrammar.INTERFACE_METHOD_OR_FIELD_REST,
        JavaGrammar.INTERFACE_GENERIC_METHOD_DECL);
  }

  private static boolean isReturningArray(AstNode node) {
    AstNode type = getType(node);
    return node.hasDirectChildren(JavaGrammar.DIM) ||
      type != null && type.hasDirectChildren(JavaGrammar.DIM);
  }

  private static boolean isReturningCollection(AstNode node) {
    AstNode type = getType(node);
    if (type == null) {
      return false;
    }

    AstNode classType = type.getFirstChild(JavaGrammar.CLASS_TYPE);
    if (classType == null) {
      return false;
    }

    List<AstNode> identifiers = classType.getChildren(JavaTokenType.IDENTIFIER);
    String lastIdentifierValue = identifiers.get(identifiers.size() - 1).getTokenOriginalValue();

    return COLLECTION_TYPES.contains(lastIdentifierValue);
  }

  private static AstNode getType(AstNode node) {
    AstNode type = node.getParent().getFirstChild(JavaGrammar.TYPE);
    if(node.is(JavaGrammar.INTERFACE_GENERIC_METHOD_DECL)){
      type = node.getFirstChild(JavaGrammar.TYPE);
    }
    return type;
  }

}
