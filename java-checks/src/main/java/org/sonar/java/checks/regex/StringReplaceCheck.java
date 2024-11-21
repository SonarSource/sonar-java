/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
