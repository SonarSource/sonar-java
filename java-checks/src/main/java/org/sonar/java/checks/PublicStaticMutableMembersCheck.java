/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.SyntaxNodePredicates;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.NameCriteria;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(
  key = "S2386",
  name = "Mutable fields should not be \"public static\"",
  priority = Priority.CRITICAL,
  tags = {Tag.CWE, Tag.UNPREDICTABLE, Tag.SECURITY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.API_ABUSE)
@SqaleConstantRemediation("15min")
public class PublicStaticMutableMembersCheck extends SubscriptionBaseVisitor {

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
  private static final MethodInvocationMatcherCollection UNMODIFIABLE_METHOD_CALLS = MethodInvocationMatcherCollection.create()
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name(NameCriteria.startsWith("unmodifiable")).withNoParameterConstraint())
      // apache commons 3.X
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.collections.map.UnmodifiableMap")).name(DECORATE).withNoParameterConstraint())
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.collections.set.UnmodifiableSet")).name(DECORATE).withNoParameterConstraint())
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.collections.list.UnmodifiableList")).name(DECORATE).withNoParameterConstraint())
      // apache commons 4.X
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.collections4.map.UnmodifiableMap")).name(DECORATE).withNoParameterConstraint())
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.collections4.set.UnmodifiableSet")).name(DECORATE).withNoParameterConstraint())
    .add(MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.collections4.list.UnmodifiableList")).name(DECORATE).withNoParameterConstraint());
  private static final MethodMatcher ARRAYS_AS_LIST = MethodMatcher.create()
    .typeDefinition("java.util.Arrays").name("asList").withNoParameterConstraint();

  private static final Set<String> ACCEPTED_TYPES = ImmutableSet.of(
    "com.google.common.collect.ImmutableMap",
    "com.google.common.collect.ImmutableCollection"
  );
  
  private static final Set<String> ACCEPTED_NEW_TYPES = ImmutableSet.of(
    "org.apache.commons.collections4.list.UnmodifiableList"
  );

  private static final Set<Symbol> IMMUTABLE_CANDIDATES = new HashSet<>();
  private static final Multimap<Tree, Symbol> CLASS_IMMUTABLE_CANDIDATES = ArrayListMultimap.create();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
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
      if (isMutable(variableTree.initializer(), symbol.type())) {
        String message = "Make this member \"protected\".";
        if (owner.is(Tree.Kind.INTERFACE)) {
          message = MessageFormat.format("Move \"{0}\" to a class and lower its visibility", variableTree.simpleName().name());
        }
        addIssue(variableTree, message);
      } else {
        IMMUTABLE_CANDIDATES.add(symbol);
        CLASS_IMMUTABLE_CANDIDATES.put(owner, symbol);
      }
    }
  }

  private void checkAssignment(AssignmentExpressionTree node) {
    ExpressionTree variable = ExpressionsHelper.skipParentheses(node.variable());
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      variable = ((MemberSelectExpressionTree) variable).identifier();
    }
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) variable).symbol();
      if (IMMUTABLE_CANDIDATES.contains(symbol) && isMutable(node.expression(), symbol.type())) {
        addIssue(variable, "Make member \"" + symbol.name() + "\" \"protected\".");
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

  static boolean isMutable(@Nullable ExpressionTree initializer, Type type) {
    if (initializer == null) {
      return Iterables.any(ALWAYS_MUTABLE_TYPES, SyntaxNodePredicates.isSubtypeOf(type));
    }
    ExpressionTree expression = ExpressionsHelper.skipParentheses(initializer);
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (isAcceptedTypeOrUnmodifiableMethodCall(mit)) {
        return false;
      } else if (ARRAYS_AS_LIST.matches(mit)) {
        return !mit.arguments().isEmpty();
      }
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      return !isAcceptedType(expression.symbolType(), ACCEPTED_NEW_TYPES);
    }
    return true;
  }

  private static boolean isAcceptedTypeOrUnmodifiableMethodCall(MethodInvocationTree mit) {
    Type type = mit.symbolType();
    return type.isUnknown() || isAcceptedType(type, ACCEPTED_TYPES) || UNMODIFIABLE_METHOD_CALLS.anyMatch(mit);
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
    return type.isArray() || Iterables.any(MUTABLE_TYPES, SyntaxNodePredicates.isSubtypeOf(type));
  }
}
