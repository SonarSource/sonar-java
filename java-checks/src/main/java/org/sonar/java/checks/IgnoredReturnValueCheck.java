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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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
  private static final List<String> CHECKED_TYPES = ImmutableList.<String>builder()
      .add(JAVA_LANG_STRING)
      .add("java.lang.Boolean")
      .add("java.lang.Integer")
      .add("java.lang.Double")
      .add("java.lang.Float")
      .add("java.lang.Byte")
      .add("java.lang.Character")
      .add("java.lang.Short")
      .add("java.lang.StackTraceElement")
      .add("java.time.DayOfWeek")
      .add("java.time.Duration")
      .add("java.time.Instant")
      .add("java.time.LocalDate")
      .add("java.time.LocalDateTime")
      .add("java.time.LocalTime")
      .add("java.time.Month")
      .add("java.time.MonthDay")
      .add("java.time.OffsetDateTime")
      .add("java.time.OffsetTime")
      .add("java.time.Period")
      .add("java.time.Year")
      .add("java.time.YearMonth")
      .add("java.time.ZonedDateTime")
      .add("java.math.BigInteger")
      .add("java.math.BigDecimal")
      .add("java.util.Optional")
      .build();

  private static final Set<String> EXCLUDED_PREFIX = ImmutableSet.of("parse", "format", "decode", "valueOf");

  private static final MethodMatcherCollection EXCLUDED = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.lang.Character").name("toChars").parameters("int", "char[]", "int"),
    MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("intern").withoutParameter());

  private static final MethodMatcher STRING_GET_BYTES = MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("getBytes").parameters("java.nio.charset.Charset");

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
      Symbol methodSymbol = mit.symbol();
      if (!isVoidOrUnknown(mit.symbolType())
          && isCheckedType(methodSymbol.owner().type())
          && methodSymbol.isPublic()
          && !isConstructor(methodSymbol)) {
        IdentifierTree methodName = ExpressionUtils.methodName(mit);
        reportIssue(methodName, "The return value of \"" + methodName.name() + "\" must be used.");
      }
    }
  }

  private static boolean isExcluded(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    return mit.symbol().isUnknown() || EXCLUDED.anyMatch(mit) ||
      (isInTryCatch(mit) && (EXCLUDED_PREFIX.stream().anyMatch(methodName::startsWith) || STRING_GET_BYTES.matches(mit)));
  }

  private static boolean isInTryCatch(MethodInvocationTree mit) {
    Tree parent =  mit.parent();
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
    return ((JavaSymbol.MethodJavaSymbol) methodSymbol).isConstructor();
  }
}
