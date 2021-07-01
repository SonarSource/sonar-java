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
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S2201")
public class IgnoredReturnValueCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
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
      .ofSubTypes("java.util.stream.Stream")
      .names("toArray", "reduce", "collect", "min", "max", "count", "anyMatch", "allMatch", "noneMatch", "findFirst", "findAny", "toList")
      .withAnyParameters()
      .build());

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
    Symbol symbol = mit.symbol();
    return !isVoidOrUnknown(mit.symbolType())
      && !isConstructor(symbol)
      && symbol.isPublic()
      && (isCheckedType(symbol.owner().type()) || COLLECTION_METHODS.matches(symbol));
  }

  private static boolean isExcluded(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    return mit.symbol().isUnknown() || EXCLUDED.matches(mit) ||
      (isInTryCatch(mit) && (EXCLUDED_PREFIX.stream().anyMatch(methodName::startsWith) || STRING_GET_BYTES.matches(mit)));
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
