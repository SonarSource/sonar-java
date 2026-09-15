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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9397")
public class StringFormatCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final String LOCALE = "java.util.Locale";
  private static final String OBJECT_ARRAY = "java.lang.Object[]";
  private static final MethodMatchers STRING_FORMAT = MethodMatchers.create()
    .ofTypes(STRING)
    .names("format")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return STRING_FORMAT;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree invocation) {
    List<Type> parameterTypes = invocation.methodSymbol().parameterTypes();
    if (parameterTypes.size() != 2 && parameterTypes.size() != 3) {
      return;
    }
    boolean hasLocale = parameterTypes.size() == 3 && parameterTypes.get(0).is(LOCALE);
    if (!hasLocale && parameterTypes.size() != 2 || hasLocale && !parameterTypes.get(1).is(STRING)) {
      return;
    }
    int formatIndex = hasLocale ? 1 : 0;
    if (invocation.arguments().size() <= formatIndex || invocation.arguments().get(formatIndex).is(Tree.Kind.IDENTIFIER)
      && invocation.arguments().get(formatIndex).symbolType().isUnknown()) {
      return;
    }
    ExpressionTree formatArgument = invocation.arguments().get(formatIndex);
    if (!formatArgument.is(Tree.Kind.STRING_LITERAL)) {
      return;
    }
    LiteralTree literal = (LiteralTree) formatArgument;
    int placeholders = countSimplePlaceholders(literal.value());
    if (placeholders == 0 || placeholders < 0 || invocation.arguments().size() != formatIndex + placeholders + 1) {
      return;
    }
    ExpressionTree lastArgument = invocation.arguments().get(invocation.arguments().size() - 1);
    if (lastArgument.symbolType().isArray() && lastArgument.symbolType().is(OBJECT_ARRAY)) {
      return;
    }
    reportIssue(invocation.methodSelect(), "Use String.valueOf() or string concatenation instead of String.format().");
  }

  private static int countSimplePlaceholders(String value) {
    int placeholders = 0;
    for (int index = 0; index < value.length(); index++) {
      if (value.charAt(index) != '%') {
        continue;
      }
      if (index + 1 >= value.length()) {
        return -1;
      }
      char conversion = value.charAt(++index);
      if (conversion == 's') {
        placeholders++;
      } else if (conversion != '%') {
        return -1;
      }
    }
    return placeholders;
  }
}
