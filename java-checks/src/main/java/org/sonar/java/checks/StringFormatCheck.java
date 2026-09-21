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
import org.sonar.java.model.LiteralUtils;
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
    boolean hasLocale = parameterTypes.size() == 3 && parameterTypes.get(0).is(LOCALE);
    if (!hasLocale && parameterTypes.size() != 2) {
      return;
    }
    int formatIndex = hasLocale ? 1 : 0;
    if (invocation.arguments().size() <= formatIndex) {
      return;
    }
    ExpressionTree formatArgument = invocation.arguments().get(formatIndex);
    if (!formatArgument.is(Tree.Kind.STRING_LITERAL)) {
      return;
    }
    LiteralTree literal = (LiteralTree) formatArgument;
    String rawValue = LiteralUtils.trimQuotes(literal.value());
    if (rawValue.contains("\\")) {
      return;
    }
    int placeholders = countSimplePlaceholders(rawValue);
    if (placeholders <= 0 || invocation.arguments().size() != formatIndex + placeholders + 1) {
      return;
    }
    List<ExpressionTree> valueArguments = invocation.arguments().subList(formatIndex + 1, invocation.arguments().size());
    for (ExpressionTree arg : valueArguments) {
      if (arg.symbolType().isArray()) {
        return;
      }
    }
    if (valueArguments.stream().anyMatch(arg -> arg.symbolType().isSubtypeOf("java.util.Formattable"))) {
      return;
    }
    reportIssue(invocation.methodSelect(), "Use String.valueOf() or string concatenation instead of String.format().");
  }

  private static int countSimplePlaceholders(String value) {
    int placeholders = 0;
    int index = 0;
    while (index < value.length()) {
      if (value.charAt(index) != '%') {
        index++;
        continue;
      }
      index++;
      if (index >= value.length()) {
        return -1;
      }
      char conversion = value.charAt(index);
      index++;
      if (conversion == 's') {
        placeholders++;
      } else if (conversion != '%') {
        return -1;
      }
    }
    return placeholders;
  }
}
