/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VarTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/** Static checks behind the redundant cast warning: widening, promotion and the casts that change overload or target typing. */
final class CastChecks {

  private static final Pattern LEADING_DIGITS = Pattern.compile("^\\d++");

  private static final Map<String, Integer> NUMERIC_RANK = Map.of("byte", 0, "short", 1, "char", 1, "int", 2, "long", 3, "float", 4, "double", 5);

  private CastChecks() {
  }

  /** Whether the cast is the result of a lambda passed as an argument: it can fix the type the call infers. */
  static boolean isResultOfArgumentLambda(TypeCastTree cast) {
    Tree parent = cast.parent();
    while (parent instanceof ParenthesizedTree || parent instanceof ReturnStatementTree) {
      parent = parent.parent();
    }
    if (parent instanceof BlockTree) {
      parent = parent.parent();
    }
    return parent instanceof LambdaExpressionTree lambda && lambda.parent() instanceof Arguments;
  }

  /** The innermost element type of an array type; the type itself otherwise. */
  static Type elementType(Type type) {
    Type element = type;
    while (element.isArray()) {
      element = ((Type.ArrayType) element).elementType();
    }
    return element;
  }

  /** Whether the cast is the array an element is read from. */
  static boolean isIndexedArray(TypeCastTree cast) {
    Tree child = cast;
    Tree parent = cast.parent();
    while (parent instanceof ParenthesizedTree) {
      child = parent;
      parent = parent.parent();
    }
    return parent instanceof ArrayAccessExpressionTree access && access.expression() == child;
  }

  /** The type of the parameter {@code argument} is passed to, when no other method of the owner or its supertypes could take the call instead. */
  private static java.util.Optional<Type> soleParameterType(Arguments arguments, Tree argument) {
    Symbol.MethodSymbol method = calledMethod(arguments);
    int index = arguments.indexOf(argument);
    if (method == null || method.isUnknown() || index >= method.parameterTypes().size() || method.isVarArgsMethod() || !(method.owner() instanceof JTypeSymbol owner)) {
      return java.util.Optional.empty();
    }
    long sameArity = java.util.stream.Stream.concat(java.util.stream.Stream.of(owner.type()), owner.superTypes().stream())
      .map(Type::symbol)
      .filter(JTypeSymbol.class::isInstance)
      .flatMap(type -> ((JTypeSymbol) type).membersNamed(method.name()).stream())
      .filter(member -> member.isMethodSymbol() && ((Symbol.MethodSymbol) member).parameterTypes().size() == arguments.size())
      .map(member -> ((Symbol.MethodSymbol) member).parameterTypes().stream().map(parameter -> parameter.erasure().fullyQualifiedName()).toList())
      .distinct()
      .count();
    return sameArity == 1 ? java.util.Optional.of(method.parameterTypes().get(index)) : java.util.Optional.empty();
  }

  /** The declared return type of the method a return statement belongs to; none inside a lambda. */
  private static java.util.Optional<Type> returnedAs(Tree statement) {
    Tree enclosing = statement.parent();
    while (!(enclosing instanceof MethodTree) && !(enclosing instanceof LambdaExpressionTree)
      && !(enclosing instanceof ClassTree)) {
      enclosing = enclosing.parent();
    }
    if (enclosing instanceof MethodTree method && method.returnType() != null) {
      return java.util.Optional.of(method.returnType().symbolType());
    }
    return java.util.Optional.empty();
  }

  /** The result type of an expression lambda whose target type does not depend on inferring the call it is passed to. */
  private static java.util.Optional<Type> lambdaResult(LambdaExpressionTree lambda) {
    Symbol.MethodSymbol method = lambda.symbol();
    if (method == null || method.isUnknown()) {
      return java.util.Optional.empty();
    }
    if (lambda.parent() instanceof Arguments arguments
      && (isGenericCall(arguments) || soleParameterType(arguments, lambda).isEmpty())) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(method.returnType().type());
  }

  private static boolean isGenericCall(Arguments arguments) {
    Symbol.MethodSymbol method = calledMethod(arguments);
    return method == null || method.isParametrizedMethod() || method.owner().type().isParameterized();
  }

  @CheckForNull
  private static Symbol.MethodSymbol calledMethod(Arguments arguments) {
    if (arguments.parent() instanceof MethodInvocationTree call) {
      return call.methodSymbol();
    }
    return arguments.parent() instanceof NewClassTree creation ? creation.methodSymbol() : null;
  }

  /** A primitive cast changes the value unless it is an identity or widening conversion of the operand; a reference cast never does. */
  static boolean castKeepsValue(Type operand, Type cast, Substitution substitution) {
    return !cast.isPrimitive() || isAssignable(operand, cast, substitution);
  }

  /** Whether a value of type {@code from} is assignable to {@code to} without a cast, boxing and unboxing included. */
  static boolean isAssignable(Type from, Type to, Substitution substitution) {
    if (from.isPrimitive() && to.isPrimitive()) {
      String fromName = from.fullyQualifiedName();
      String toName = to.fullyQualifiedName();
      return fromName.equals(toName) || (NUMERIC_RANK.containsKey(fromName) && NUMERIC_RANK.containsKey(toName) && isWidening(fromName, toName));
    }
    if (from.isPrimitive()) {
      Type boxed = from.primitiveWrapperType();
      return boxed != null && boxed.isSubtypeOf(to);
    }
    if (to.isPrimitive()) {
      Type unboxed = from.primitiveType();
      return unboxed != null && isAssignable(unboxed, to, substitution);
    }
    return from.isSubtypeOf(to) && substitution.preservesArguments(from, to);
  }

  /** The type as the compiler prints it: type arguments included, without spaces between them. */
  static String displayName(Type type) {
    if (type.isNullType()) {
      return "null";
    }
    if (type.isArray()) {
      return displayName(((Type.ArrayType) type).elementType()) + "[]";
    }
    if (type.isParameterized()) {
      return memberName(type.erasure()) + type.typeArguments().stream().map(CastChecks::displayName).collect(java.util.stream.Collectors.joining(",", "<", ">"));
    }
    return memberName(type);
  }

  /**
   * The bounds of an intersection type in the order the compiler keeps them: as written when the first one is a class,
   * otherwise sorted by qualified name.
   */
  static String intersectionName(List<Type> bounds) {
    List<Type> ordered = new ArrayList<>(bounds);
    if (!isClassType(ordered.get(0))) {
      ordered.sort(java.util.Comparator.comparing(CastChecks::readableName));
    }
    return ordered.stream().map(CastChecks::displayName).collect(java.util.stream.Collectors.joining(" & "));
  }

  private static boolean isClassType(Type type) {
    return type.isClass() && !type.symbol().isInterface();
  }

  private static String readableName(Type type) {
    if (type.isParameterized()) {
      return readableName(type.erasure()) + type.typeArguments().stream().map(CastChecks::readableName).collect(java.util.stream.Collectors.joining(",", "<", ">"));
    }
    return type.isTypeVar() ? type.name() : type.fullyQualifiedName().replace('$', '.');
  }

  /**
   * A member type is spelled with its enclosing types, as the compiler does in its messages. A local type starts its own
   * spelling, and an anonymous one has none.
   */
  private static String memberName(Type type) {
    String fqn = type.fullyQualifiedName();
    if (fqn.indexOf('$') < 0 || type.isTypeVar() || (type instanceof JType jType && jType.isWildcard())) {
      return type.name();
    }
    List<String> names = new ArrayList<>();
    for (String segment : fqn.substring(fqn.lastIndexOf('.') + 1).split("\\$")) {
      String name = LEADING_DIGITS.matcher(segment).replaceFirst("");
      if (name.length() != segment.length()) {
        names.clear();
      }
      names.add(name);
    }
    return String.join(".", names);
  }

  /** Whether the cast is the operand of another cast that the operand's own type cannot reach without it. */
  static boolean changesTypeFor(TypeCastTree cast, Type operand, Substitution substitution) {
    Tree parent = cast.parent();
    while (parent instanceof ParenthesizedTree) {
      parent = parent.parent();
    }
    if (!(parent instanceof TypeCastTree outer)) {
      return false;
    }
    Type target = outer.type().symbolType();
    return target.isParameterized() && operand.isParameterized()
      && !substitution.preservesArguments(operand, target) && !substitution.preservesArguments(target, operand);
  }

  /** The extra bounds of an intersection cast also have to hold for the operand. */
  static boolean satisfiesBounds(TypeCastTree cast, Type operand) {
    return cast.bounds().stream().allMatch(bound -> !bound.symbolType().isUnknown() && operand.isSubtypeOf(bound.symbolType()));
  }

  /** The declared type of the variable the cast initializes or is assigned to. */
  static java.util.Optional<Type> assignedTo(TypeCastTree cast) {
    Tree child = cast;
    Tree parent = cast.parent();
    while (parent instanceof ParenthesizedTree) {
      child = parent;
      parent = parent.parent();
    }
    if (parent instanceof VariableTree variable && variable.initializer() == child
      && !(variable.type() instanceof VarTypeTree)) {
      return java.util.Optional.of(variable.type().symbolType());
    }
    if (parent instanceof AssignmentExpressionTree assignment && assignment.is(Tree.Kind.ASSIGNMENT) && assignment.expression() == child) {
      return java.util.Optional.of(assignment.variable().symbolType());
    }
    if (parent instanceof Arguments arguments) {
      return soleParameterType(arguments, child);
    }
    if (parent instanceof ReturnStatementTree) {
      return returnedAs(parent);
    }
    if (parent instanceof LambdaExpressionTree lambda && lambda.body() == child) {
      return lambdaResult(lambda);
    }
    return java.util.Optional.empty();
  }

  static String castMessage(ExpressionTree cast) {
    return "Unnecessary cast from " + displayName(((TypeCastTree) cast).expression().symbolType()) + " to " + displayName(cast.symbolType());
  }

  static boolean isWideningPrimitiveCast(ExpressionTree tree) {
    if (!(tree instanceof TypeCastTree cast)) {
      return false;
    }
    String from = cast.expression().symbolType().fullyQualifiedName();
    String to = cast.type().symbolType().fullyQualifiedName();
    return NUMERIC_RANK.containsKey(from) && NUMERIC_RANK.containsKey(to) && (from.equals(to) || isWidening(from, to));
  }

  private static boolean isWidening(String from, String to) {
    if ("char".equals(to)) {
      return false;
    }
    if ("char".equals(from)) {
      return NUMERIC_RANK.get(to) >= NUMERIC_RANK.get("int");
    }
    return NUMERIC_RANK.get(from) < NUMERIC_RANK.get(to);
  }

  /** The type the operand has once its cast is dropped, or null when the operand is not a numeric primitive. */
  static String promotionInput(ExpressionTree operand, boolean dropCast) {
    Type type = dropCast ? ((TypeCastTree) operand).expression().symbolType() : operand.symbolType();
    String name = type.fullyQualifiedName();
    return NUMERIC_RANK.containsKey(name) ? name : null;
  }

  static boolean sameOperandTypes(Tree.Kind kind, String leftWithout, String rightWithout, ExpressionTree left, ExpressionTree right) {
    String leftWith = left.symbolType().fullyQualifiedName();
    String rightWith = right.symbolType().fullyQualifiedName();
    if (kind == Tree.Kind.LEFT_SHIFT || kind == Tree.Kind.RIGHT_SHIFT || kind == Tree.Kind.UNSIGNED_RIGHT_SHIFT) {
      return unaryPromoted(leftWith).equals(unaryPromoted(leftWithout)) && unaryPromoted(rightWith).equals(unaryPromoted(rightWithout));
    }
    return binaryPromoted(leftWith, rightWith).equals(binaryPromoted(leftWithout, rightWithout));
  }

  private static String unaryPromoted(String type) {
    return NUMERIC_RANK.get(type) < NUMERIC_RANK.get("int") ? "int" : type;
  }

  private static String binaryPromoted(String left, String right) {
    String a = unaryPromoted(left);
    String b = unaryPromoted(right);
    return NUMERIC_RANK.get(a) >= NUMERIC_RANK.get(b) ? a : b;
  }
}
