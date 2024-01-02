/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.regex;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5361")
public class StringReplaceCheck extends AbstractRegexCheck {

  private static final String LANG_STRING = "java.lang.String";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes(LANG_STRING)
      .names("replaceAll")
      .addParametersMatcher(LANG_STRING, LANG_STRING)
      .build();
  }

  @Override
  protected boolean filterAnnotation(AnnotationTree annotation) {
    return false;
  }

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    RegexTree regex = regexForLiterals.getResult();
    if (!regexForLiterals.hasSyntaxErrors() && isPlainString(regex)) {
      reportIssue(ExpressionUtils.methodName((MethodInvocationTree) methodInvocationOrAnnotation),
        "Replace this call to \"replaceAll()\" by a call to the \"replace()\" method.");
    }
  }

  private static boolean isPlainString(RegexTree regex) {
    return regex.is(RegexTree.Kind.CHARACTER)
      || (regex.is(RegexTree.Kind.SEQUENCE)
      && ((SequenceTree) regex).getItems().stream().allMatch(item -> item.is(RegexTree.Kind.CHARACTER)));
  }
}
