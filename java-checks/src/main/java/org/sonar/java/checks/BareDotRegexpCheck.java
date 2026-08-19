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

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S9353")
public class BareDotRegexpCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "This regex \".\" matches any character, not a literal dot; escape it as \"\\\\.\" if a period was intended.";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_REGEX_PATTERN = "java.util.regex.Pattern";
  private static final String COMPILE_METHOD_NAME = "compile";

  private static final MethodMatchers REGEX_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("split", "matches")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("split")
      .addParametersMatcher(JAVA_LANG_STRING, "int")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("replaceAll", "replaceFirst")
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_REGEX_PATTERN)
      .names("matches")
      .addParametersMatcher(JAVA_LANG_STRING, "java.lang.CharSequence")
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_REGEX_PATTERN)
      .names(COMPILE_METHOD_NAME)
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_UTIL_REGEX_PATTERN)
      .names(COMPILE_METHOD_NAME)
      .addParametersMatcher(JAVA_LANG_STRING, "int")
      .build());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return REGEX_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree regexArgument = mit.arguments().get(0);
    if (isBareDot(regexArgument) && !isLiteralPattern(mit)) {
      reportIssue(regexArgument, MESSAGE);
    }
  }

  private static boolean isBareDot(ExpressionTree regexArgument) {
    return regexArgument.asConstant(String.class).filter("."::equals).isPresent();
  }

  private static boolean isLiteralPattern(MethodInvocationTree mit) {
    if (mit.arguments().size() < 2 || !COMPILE_METHOD_NAME.equals(mit.methodSymbol().name())) {
      return false;
    }
    return mit.arguments().get(1).asConstant(Integer.class)
      .map(flags -> (flags & Pattern.LITERAL) != 0)
      .orElse(true);
  }

}
