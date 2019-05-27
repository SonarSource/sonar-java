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
package org.sonar.java.checks.spring;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4601")
public class SpringAntMatcherOrderCheck extends AbstractMethodDetection {

  private static final Pattern MATCHER_SPECIAL_CHAR = Pattern.compile("[?*{]");

  private static final MethodMatcher ANT_MATCHERS = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry"))
    .name("antMatchers")
    .addParameter("java.lang.String[]");

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create()
      .typeDefinition(TypeCriteria.is("org.springframework.security.config.annotation.web.builders.HttpSecurity"))
      .name("authorizeRequests")
      .withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree method) {
    List<StringConstant> antPatterns = collectAntPatterns(method);
    for (int indexToCheck = 1; indexToCheck < antPatterns.size(); indexToCheck++) {
      checkAntPatternAt(antPatterns, indexToCheck);
    }
  }

  private void checkAntPatternAt(List<StringConstant> antPatterns, int indexToCheck) {
    StringConstant patternToCheck = antPatterns.get(indexToCheck);
    StringConstant incompatiblePattern = firstIncompatiblePreviousPattern(patternToCheck, antPatterns, indexToCheck);
    if (incompatiblePattern != null) {
      List<JavaFileScannerContext.Location> secondary = Collections.singletonList(
        new JavaFileScannerContext.Location("Less restrictive", incompatiblePattern.expression));

      reportIssue(patternToCheck.expression, "Reorder the URL patterns from most to less specific, the pattern \"" +
        patternToCheck.value + "\" should occurs before \"" + incompatiblePattern.value + "\".", secondary, null);
    }
  }

  @CheckForNull
  private static StringConstant firstIncompatiblePreviousPattern(StringConstant patternToCheck, List<StringConstant> antPatterns, int antPatternsSize) {
    for (int i = 0; i < antPatternsSize; i++) {
      StringConstant previousPattern = antPatterns.get(i);
      if (matches(previousPattern.value, patternToCheck.value)) {
        return previousPattern;
      }
    }
    return null;
  }

  private static List<StringConstant> collectAntPatterns(MethodInvocationTree method) {
    List<StringConstant> antPatterns = new ArrayList<>();
    MethodInvocationTree parentMethod = parentMethodInvocation(method);
    while (parentMethod != null) {
      if (ANT_MATCHERS.matches(parentMethod)) {
        antPatterns.addAll(antMatchersPatterns(parentMethod));
      }
      parentMethod = parentMethodInvocation(parentMethod);
    }
    return antPatterns;
  }

  @CheckForNull
  private static MethodInvocationTree parentMethodInvocation(MethodInvocationTree method) {
    Tree parent = method.parent();
    if (parent.is(Tree.Kind.MEMBER_SELECT)) {
      parent = parent.parent();
      if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
        return (MethodInvocationTree) parent;
      }
    }
    return null;
  }

  @VisibleForTesting
  static boolean matches(String pattern, String text) {
    if (pattern.equals(text)) {
      return true;
    }
    if (pattern.endsWith("**") && text.startsWith(pattern.substring(0, pattern.length() - 2))) {
      return true;
    }
    boolean antPatternContainsRegExp = pattern.contains("{");
    boolean textIsAlsoAnAntPattern = MATCHER_SPECIAL_CHAR.matcher(text).find();
    if (pattern.isEmpty() || antPatternContainsRegExp || textIsAlsoAnAntPattern) {
      return false;
    }
    return text.matches(antMatcherToRegEx(pattern));
  }

  @VisibleForTesting
  static String antMatcherToRegEx(String pattern) {
    // Note, regexp is not supported: {spring:[a-z]+} matches the regexp [a-z]+ as a path variable named "spring"

    // escape regexp special characters
    return escapeRegExpChars(pattern)
      // ? matches one character
      .replace("?", "[^/]")
      // ** matches zero or more directories in a path ("$$" is a temporary place holder)
      .replace("**", "$$")
      // * matches zero or more characters
      .replace("*", "[^/]*")
      .replace("$$", ".*");
  }

  @VisibleForTesting
  static String escapeRegExpChars(String pattern) {
    return pattern.replaceAll("([.(){}+|^$\\[\\]\\\\])", "\\\\$1");
  }

  private static List<StringConstant> antMatchersPatterns(MethodInvocationTree mit) {
    return mit.arguments().stream()
      .map(StringConstant::of)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private static class StringConstant {
    private final ExpressionTree expression;
    private final String value;

    private StringConstant(ExpressionTree expression, String value) {
      this.expression = expression;
      this.value = value;
    }

    @CheckForNull
    private static StringConstant of(ExpressionTree expression) {
      String value = ExpressionsHelper.getConstantValueAsString(expression).value();
      if (value != null) {
        return new StringConstant(expression, value);
      }
      return null;
    }
  }

}
