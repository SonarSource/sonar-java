/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S2201")
public class IgnoredReturnValueCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_FUNCTION_SUPPLIER = "java.util.function.Supplier";
  private static final String JAVA_UTIL_STREAM_STREAM = "java.util.stream.Stream";
  private static final String COLLECT = "collect";
  private static final List<String> CHECKED_TYPES = Arrays.asList(
    JAVA_LANG_STRING,
    "java.lang.Boolean",
    "java.lang.Integer",
    "java.lang.Double",
    "java.lang.Float",
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.StackTraceElement",
    "java.time.DayOfWeek",
    "java.time.Duration",
    "java.time.Instant",
    "java.time.LocalDate",
    "java.time.LocalDateTime",
    "java.time.LocalTime",
    "java.time.Month",
    "java.time.MonthDay",
    "java.time.OffsetDateTime",
    "java.time.OffsetTime",
    "java.time.Period",
    "java.time.Year",
    "java.time.YearMonth",
    "java.time.ZonedDateTime",
    "java.math.BigInteger",
    "java.math.BigDecimal",
    "java.util.Optional",
    "com.google.common.base.Optional");

  private static final List<String> EXCLUDED_PREFIX = Arrays.asList("parse", "format", "decode", "valueOf");

  private static final MethodMatchers EXCLUDED = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.lang.Character").names("toChars").addParametersMatcher("int", "char[]", "int").build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_STRING).names("intern").addWithoutParametersMatcher().build());

  private static final MethodMatchers STRING_GET_BYTES = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING).names("getBytes").addParametersMatcher("java.nio.charset.Charset").build();

  private static final MethodMatchers COLLECTION_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.util.Collection")
      .names("size", "isEmpty", "contains", "containsAll", "iterator")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.Collection")
      .names("toArray")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.Map")
      .names("get", "getOrDefault", "size", "isEmpty", "containsKey", "containsValue", "keySet", "entrySet", "values")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes(JAVA_UTIL_STREAM_STREAM)
      .names(COLLECT, "toArray", "reduce", "min", "max", "count", "anyMatch", "allMatch", "noneMatch", "findFirst", "findAny", "toList")
      .withAnyParameters()
      .build());

  private static final MethodMatchers COLLECT_WITH_COLLECTOR = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_STREAM_STREAM)
    .names(COLLECT)
    .addParametersMatcher("java.util.stream.Collector")
    .build();

  private static final MethodMatchers COLLECT_WITH_FUNCTIONS = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_STREAM_STREAM)
    .names(COLLECT)
    .addParametersMatcher(JAVA_UTIL_FUNCTION_SUPPLIER, "java.util.function.BiConsumer", "java.util.function.BiConsumer")
    .build();

  private static final MethodMatchers TO_COLLECTION = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Collectors")
    .names("toCollection")
    .addParametersMatcher(JAVA_UTIL_FUNCTION_SUPPLIER)
    .build();

  private static final MethodMatchers TO_MAP_WITH_SUPPLIER = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Collectors")
    .names("toMap")
    .addParametersMatcher("java.util.function.Function", "java.util.function.Function", "java.util.function.BinaryOperator",
      JAVA_UTIL_FUNCTION_SUPPLIER)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expr = ((ExpressionStatementTree) tree).expression();
    if (expr.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expr;
      if (isExcluded(mit)) {
        return;
      }
      if (shouldUseReturnValue(mit)) {
        IdentifierTree methodName = ExpressionUtils.methodName(mit);
        reportIssue(methodName, "The return value of \"" + methodName.name() + "\" must be used.");
      }
    }
  }

  private static boolean shouldUseReturnValue(MethodInvocationTree mit) {
    Symbol symbol = mit.methodSymbol();
    return !isVoidOrUnknown(mit.symbolType())
      && !isConstructor(symbol)
      && symbol.isPublic()
      && (isCheckedType(symbol.owner().type()) || COLLECTION_METHODS.matches(symbol));
  }

  private static boolean isExcluded(MethodInvocationTree mit) {
    String methodName = mit.methodSymbol().name();
    return mit.methodSymbol().isUnknown() || EXCLUDED.matches(mit) || mayBeCollectingIntoVariable(mit) ||
      (isInTryCatch(mit) && (EXCLUDED_PREFIX.stream().anyMatch(methodName::startsWith) || STRING_GET_BYTES.matches(mit)));
  }

  /**
   * Returns true if Stream.collect is invoked with a supplier that's not a constructor. This is meant to exclude cases
   * like `stream.collect(Collectors.toCollection(() -> var))`, which write their results into a variable and thus don't
   * have to have their return values used.
   */
  private static boolean mayBeCollectingIntoVariable(MethodInvocationTree mit) {
    if (COLLECT_WITH_FUNCTIONS.matches(mit)) {
      return !isConstructor(mit.arguments().get(0));
    }
    if (COLLECT_WITH_COLLECTOR.matches(mit)) {
      ExpressionTree arg = mit.arguments().get(0);
      if (!arg.is(Tree.Kind.METHOD_INVOCATION)) {
        return false;
      }
      MethodInvocationTree collector = (MethodInvocationTree) arg;
      if (TO_COLLECTION.matches(collector)) {
        return !isConstructor(collector.arguments().get(0));
      }
      if (TO_MAP_WITH_SUPPLIER.matches(collector)) {
        return !isConstructor(collector.arguments().get(3));
      }
    }
    return false;
  }

  private static boolean isConstructor(ExpressionTree tree) {
    if (tree.is(Tree.Kind.METHOD_REFERENCE)) {
      return "new".equals(((MethodReferenceTree) tree).method().name());
    }
    return (tree.is(Tree.Kind.LAMBDA_EXPRESSION))
      && ((LambdaExpressionTree) tree).body().is(Tree.Kind.NEW_CLASS, Tree.Kind.NEW_ARRAY);
  }

  private static boolean isInTryCatch(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    while (parent != null && !parent.is(Tree.Kind.TRY_STATEMENT)) {
      parent = parent.parent();
    }
    return parent != null && !((TryStatementTree) parent).catches().isEmpty();
  }

  private static boolean isCheckedType(Type ownerType) {
    return CHECKED_TYPES.stream().anyMatch(ownerType::is);
  }

  private static boolean isVoidOrUnknown(Type methodType) {
    return methodType.isVoid() || methodType.isUnknown();
  }

  private static boolean isConstructor(Symbol methodSymbol) {
    return "<init>".equals(methodSymbol.name());
  }
}
