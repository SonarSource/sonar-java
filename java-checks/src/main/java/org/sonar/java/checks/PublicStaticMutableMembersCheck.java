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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2386")
public class PublicStaticMutableMembersCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> ALWAYS_MUTABLE_TYPES = ImmutableSet.of(
    "java.awt.Point",
    "java.util.Date"
  );

  private static final Set<String> MUTABLE_TYPES = ImmutableSet.<String>builder()
    .addAll(ALWAYS_MUTABLE_TYPES)
    .add("java.util.Collection")
    .add("java.util.Map")
    .build();

  private static final String DECORATE = "decorate";
  // java.util and apache commons
  private static final MethodMatchers UNMODIFIABLE_METHOD_CALLS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.util.Collections").name(name -> name.startsWith("singleton") || name.startsWith("empty")).withAnyParameters().build(),
    MethodMatchers.create().ofAnyType().name(name -> name.startsWith("unmodifiable")).withAnyParameters().build(),
    // Java 9s
    MethodMatchers.create().ofTypes("java.util.Set", "java.util.List", "java.util.Map").names("of").withAnyParameters().build(),
    // apache...
    MethodMatchers.create()
      // commons 3.X
      .ofSubTypes(
        "org.apache.commons.collections.map.UnmodifiableMap",
        "org.apache.commons.collections.list.UnmodifiableList",
        "org.apache.commons.collections.set.UnmodifiableSet",
        // commons 4.X
        "org.apache.commons.collections4.map.UnmodifiableMap",
        "org.apache.commons.collections4.set.UnmodifiableSet",
        "org.apache.commons.collections4.list.UnmodifiableList")
      .names(DECORATE)
      .withAnyParameters()
      .build()
  );

  private static final MethodMatchers ARRAYS_AS_LIST = MethodMatchers.create()
    .ofTypes("java.util.Arrays").names("asList").withAnyParameters().build();

  private static final Set<String> ACCEPTED_TYPES = ImmutableSet.of(
    "com.google.common.collect.ImmutableMap",
    "com.google.common.collect.ImmutableCollection"
  );

  private static final Set<String> ACCEPTED_NEW_TYPES = Collections.singleton(
    "org.apache.commons.collections4.list.UnmodifiableList"
  );

  private static final Set<Symbol> IMMUTABLE_CANDIDATES = new HashSet<>();
  private static final Multimap<Tree, Symbol> CLASS_IMMUTABLE_CANDIDATES = ArrayListMultimap.create();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    CLASS_IMMUTABLE_CANDIDATES.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      checkAssignment((AssignmentExpressionTree) tree);
    } else {
      List<Tree> members = ((ClassTree) tree).members();
      for (Tree member : members) {
        if (member.is(Tree.Kind.VARIABLE)) {
          preCheckVariable(tree, (VariableTree) member);
        }
      }
    }
  }

  private void preCheckVariable(Tree owner, VariableTree variableTree) {
    Symbol symbol = variableTree.symbol();
    if (symbol != null && isPublicStatic(symbol) && isForbiddenType(symbol.type())) {
      if (isMutable(variableTree.initializer(), symbol)) {
        String message = "Make this member \"protected\".";
        if (owner.is(Tree.Kind.INTERFACE)) {
          message = MessageFormat.format("Move \"{0}\" to a class and lower its visibility", variableTree.simpleName().name());
        }
        reportIssue(variableTree.simpleName(), message);
      } else {
        IMMUTABLE_CANDIDATES.add(symbol);
        CLASS_IMMUTABLE_CANDIDATES.put(owner, symbol);
      }
    }
  }

  private void checkAssignment(AssignmentExpressionTree node) {
    ExpressionTree variable = ExpressionUtils.skipParentheses(node.variable());
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      variable = ((MemberSelectExpressionTree) variable).identifier();
    }
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) variable;
      Symbol symbol = identifierTree.symbol();
      if (IMMUTABLE_CANDIDATES.contains(symbol) && isMutable(node.expression(), symbol)) {
        reportIssue(identifierTree, "Make member \"" + symbol.name() + "\" \"protected\".");
        IMMUTABLE_CANDIDATES.remove(symbol);
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    // cleanup
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM)) {
      IMMUTABLE_CANDIDATES.removeAll(CLASS_IMMUTABLE_CANDIDATES.get(tree));
    }
  }

  static boolean isMutable(@Nullable ExpressionTree initializer, Symbol symbol) {
    Type type = symbol.type();
    if (initializer == null) {
      return ALWAYS_MUTABLE_TYPES.stream().anyMatch(type::isSubtypeOf);
    }
    if (symbol.isFinal() && isEmptyArray(initializer)) {
      return false;
    }
    ExpressionTree expression = ExpressionUtils.skipParentheses(initializer);
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      return returnValueIsMutable((MethodInvocationTree) expression);
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      return !isAcceptedType(expression.symbolType(), ACCEPTED_NEW_TYPES);
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol assigned = ((IdentifierTree) expression).symbol();
      return !IMMUTABLE_CANDIDATES.contains(assigned);
    }
    return true;
  }

  private static boolean isEmptyArray(ExpressionTree expression) {
    if (!expression.is(Tree.Kind.NEW_ARRAY)) {
      return false;
    }
    NewArrayTree nat = (NewArrayTree) expression;
    return hasEmptyInitializer(nat) || hasOnlyZeroDimensions(nat.dimensions());
  }

  private static boolean hasEmptyInitializer(NewArrayTree newArrayTree) {
    return newArrayTree.openBraceToken() != null && newArrayTree.initializers().isEmpty();
  }

  private static boolean hasOnlyZeroDimensions(List<ArrayDimensionTree> dimensions) {
    return !dimensions.isEmpty() && dimensions.stream().allMatch(PublicStaticMutableMembersCheck::isZeroDimension);
  }

  private static boolean isZeroDimension(ArrayDimensionTree dim) {
    ExpressionTree expression = dim.expression();
    return expression != null && expression.is(Tree.Kind.INT_LITERAL) && "0".equals(((LiteralTree) expression).value());
  }

  private static boolean returnValueIsMutable(MethodInvocationTree mit) {
    if (isAcceptedTypeOrUnmodifiableMethodCall(mit)) {
      return false;
    } else if (ARRAYS_AS_LIST.matches(mit)) {
      return !mit.arguments().isEmpty();
    }
    return true;
  }

  private static boolean isAcceptedTypeOrUnmodifiableMethodCall(MethodInvocationTree mit) {
    Type type = mit.symbolType();
    return type.isUnknown() || isAcceptedType(type, ACCEPTED_TYPES) || UNMODIFIABLE_METHOD_CALLS.matches(mit);
  }

  private static boolean isAcceptedType(Type type, Set<String> accepted) {
    for (String acceptedType : accepted) {
      if (type.isSubtypeOf(acceptedType)) {
        return true;
      }
    }
    return false;
  }

  static boolean isPublicStatic(Symbol symbol) {
    return symbol.isStatic() && symbol.isPublic();
  }

  static boolean isForbiddenType(final Type type) {
    return type.isArray() || MUTABLE_TYPES.stream().anyMatch(type::isSubtypeOf);
  }
}
