/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
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
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2386")
public class PublicStaticMutableMembersCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ALWAYS_MUTABLE_TYPES = Arrays.asList(
    "java.awt.Point",
    "java.util.Date"
  );

  private static final List<String> MUTABLE_TYPES = Arrays.asList(
    "java.awt.Point",
    "java.util.Date",
    "java.util.Collection",
    "java.util.Map");

  private static final String DECORATE = "decorate";
  // java.util and apache commons
  private static final MethodMatchers UNMODIFIABLE_METHOD_CALLS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.util.Collections").name(name -> name.startsWith("singleton") || name.startsWith("empty")).withAnyParameters().build(),
    MethodMatchers.create().ofType(type -> MutableMembersUsageCheck.containsImmutableLikeTerm(type.name())).anyName().withAnyParameters().build(),
    MethodMatchers.create().ofAnyType().name(MutableMembersUsageCheck::containsImmutableLikeTerm).withAnyParameters().build(),
    // Java 9s
    MethodMatchers.create().ofTypes("java.util.Set", "java.util.List", "java.util.Map").names("of", "ofEntries", "copyOf").withAnyParameters().build(),
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
      .build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Sets").names("union", "intersection", "difference", "symmetricDifference").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Lists").names("asList").withAnyParameters().build()
  );

  private static final MethodMatchers STREAM_COLLECT_CALL = MethodMatchers.create().
    ofTypes("java.util.stream.Stream")
    .names("collect")
    .addParametersMatcher("java.util.stream.Collector")
    .build();

  private static final MethodMatchers UNMODIFIABLE_COLLECTOR_CALL = MethodMatchers.create().
    ofTypes("java.util.stream.Collectors")
    .names("toUnmodifiableSet", "toUnmodifiableList", "toUnmodifiableMap")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ARRAYS_AS_LIST = MethodMatchers.create()
    .ofTypes("java.util.Arrays").names("asList").withAnyParameters().build();

  private static final List<String> ACCEPTED_TYPES = Arrays.asList(
    "com.google.common.collect.ImmutableMap",
    "com.google.common.collect.ImmutableCollection"
  );

  private static final List<String> ACCEPTED_NEW_TYPES = Collections.singletonList(
    "org.apache.commons.collections4.list.UnmodifiableList"
  );

  private static final Set<Symbol> IMMUTABLE_CANDIDATES = new HashSet<>();
  private static final Map<Tree, List<Symbol>> CLASS_IMMUTABLE_CANDIDATES = new HashMap<>();

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
        CLASS_IMMUTABLE_CANDIDATES.computeIfAbsent(owner, key -> new ArrayList<>()).add(symbol);
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
      IMMUTABLE_CANDIDATES.removeAll(CLASS_IMMUTABLE_CANDIDATES.getOrDefault(tree, Collections.emptyList()));
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
      return !isUnknownOrAcceptedType(expression.symbolType(), ACCEPTED_NEW_TYPES);
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
    return expression != null && LiteralUtils.isZero(expression);
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
    return isUnknownOrAcceptedType(type, ACCEPTED_TYPES) || UNMODIFIABLE_METHOD_CALLS.matches(mit) || isUnmodifiableCollector(mit);
  }

  private static boolean isUnmodifiableCollector(MethodInvocationTree methodInvocationTree) {
    if (STREAM_COLLECT_CALL.matches(methodInvocationTree) && methodInvocationTree.arguments().get(0).is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree collector = (MethodInvocationTree) methodInvocationTree.arguments().get(0);
      return UNMODIFIABLE_COLLECTOR_CALL.matches(collector);
    }
    return false;
  }

  private static boolean isUnknownOrAcceptedType(Type type, List<String> accepted) {
    // In case of broken semantics, the type is unknown and can therefore not be matched against an accepted one.
    // To avoid raising FPs, we consider that an unknown type is most likely an accepted one.
    if (type.isUnknown()) {
      return true;
    }
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
