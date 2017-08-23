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
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S2201")
public class IgnoredReturnValueCheck extends IssuableSubscriptionVisitor {

  private static final List<String> CHECKED_TYPES = ImmutableList.<String>builder()
      .add("java.lang.String")
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

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionStatementTree est = (ExpressionStatementTree) tree;
    if (est.expression().is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) est.expression();
      if (!isVoidOrUnknown(mit.symbolType()) && isCheckedType(mit.symbol().owner().type())
        && mit.symbol().isPublic() && !((JavaSymbol.MethodJavaSymbol) mit.symbol()).isConstructor()) {
        IdentifierTree methodName = MethodsHelper.methodName(mit);
        reportIssue(methodName, "The return value of \"" + methodName.name() + "\" must be used.");
      }
    }
  }

  private static boolean isCheckedType(Type ownerType) {
    return CHECKED_TYPES.stream().anyMatch(ownerType::is);
  }

  private static boolean isVoidOrUnknown(Type methodType) {
    return methodType.isVoid() || methodType.isUnknown();
  }

}
