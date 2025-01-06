/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.finders.ImpossibleBackReferenceFinder;

@Rule(key = "S6001")
public class ImpossibleBackReferenceCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new ImpossibleBackReferenceFinder(this::reportIssueFromCommons).visit(regexForLiterals);
  }

}
