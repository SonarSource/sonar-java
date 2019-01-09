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

import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S3986")
public class DateFormatWeekYearCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create().typeDefinition("java.text.SimpleDateFormat").name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.arguments().isEmpty()) {
      return;
    }
    ExpressionTree expressionTree = newClassTree.arguments().get(0);
    if (!expressionTree.is(Tree.Kind.STRING_LITERAL)) {
      return;
    }
    String datePattern = LiteralUtils.trimQuotes(((LiteralTree) expressionTree).value());
    if (!StringUtils.contains(datePattern, 'w')) {
      int start = datePattern.indexOf('Y');
      if (start > -1) {
        int end = start;
        while (end < datePattern.length() && datePattern.charAt(end) == 'Y') {
          end++;
        }
        String firstYseq = datePattern.substring(start, end);
        reportIssue(expressionTree, "Make sure that Week Year \"" + firstYseq + "\" is expected here instead of Year \"" + firstYseq.toLowerCase() + "\".");
      }
    }
  }
}
