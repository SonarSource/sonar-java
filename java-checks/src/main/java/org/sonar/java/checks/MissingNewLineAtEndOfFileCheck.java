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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00113", repositoryKey = "squid")
@Rule(key = "S113")
public class MissingNewLineAtEndOfFileCheck implements JavaFileScanner {


  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (isEmptyOrNotEndingWithNewLine(context.getFileContent())) {
      context.addIssueOnFile(this, "Add a new line at the end of this file.");
    }
  }

  private static boolean isEmptyOrNotEndingWithNewLine(String content) {
    if (content.isEmpty()) {
      return true;
    }
    char lastChar = content.charAt(content.length() - 1);
    return lastChar != '\n' && lastChar != '\r';
  }
}
