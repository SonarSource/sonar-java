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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S9346")
public class IntegerToLongTimestampCastCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Use a \"long\" value to represent this timestamp.";

  private static final MethodMatchers CONSTRUCTOR_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.Date")
      .constructor()
      .addParametersMatcher("long")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.sql.Timestamp")
      .constructor()
      .addParametersMatcher("long")
      .build());

  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.time.Instant")
      .names("ofEpochSecond")
      .addParametersMatcher("long")
      .addParametersMatcher("long", "long")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.time.Instant")
      .names("ofEpochMilli")
      .addParametersMatcher("long")
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.Calendar")
      .names("setTimeInMillis")
      .addParametersMatcher("long")
      .build());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(CONSTRUCTOR_MATCHERS, METHOD_MATCHERS);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    checkArgument(mit.arguments().get(0));
  }

  @Override
  protected void onConstructorFound(NewClassTree nct) {
    checkArgument(nct.arguments().get(0));
  }

  private void checkArgument(ExpressionTree argument) {
    ExpressionTree arg = ExpressionUtils.skipParentheses(argument);
    if (arg.is(Tree.Kind.TYPE_CAST)) {
      arg = ((TypeCastTree) arg).expression();
    }
    if (arg.is(Tree.Kind.INT_LITERAL)) {
      return;
    }
    Type type = arg.symbolType();
    if (type.isUnknown()) {
      return;
    }
    if (isNarrowIntegerType(type)) {
      reportIssue(argument, MESSAGE);
    }
  }

  private static boolean isNarrowIntegerType(Type type) {
    return type.isPrimitive(Type.Primitives.INT)
      || type.isPrimitive(Type.Primitives.SHORT)
      || type.isPrimitive(Type.Primitives.BYTE)
      || type.isPrimitive(Type.Primitives.CHAR);
  }
}
