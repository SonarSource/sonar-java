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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5361")
public class StringReplaceCheck extends AbstractMethodDetection {

  private static final String LANG_STRING = "java.lang.String";
  private static final char[] REGEX_META = ".$|([{^?*+\\".toCharArray();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create().typeDefinition(LANG_STRING)
      .name("replaceAll")
      .addParameter(LANG_STRING)
      .addParameter(LANG_STRING)
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree regexArg = mit.arguments().get(0);
    String regexValue = ConstantUtils.resolveAsStringConstant(regexArg);
    if (regexValue != null && !isRegex(regexValue)) {
      reportIssue(((MemberSelectExpressionTree) mit.methodSelect()).identifier(), "Replace this call to \"replaceAll()\" by a call to the \"replace()\" method.");
    }
  }

  private static boolean isRegex(String s) {
    for (char c : s.toCharArray()) {
      for (char meta : REGEX_META) {
        if (c == meta) {
          return true;
        }
      }
    }
    return false;
  }
}
