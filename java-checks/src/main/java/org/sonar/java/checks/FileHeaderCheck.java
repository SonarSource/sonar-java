/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(key = "S1451")
public class FileHeaderCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_HEADER_FORMAT = "";
  private static final String MESSAGE = "Add or update the header of this file.";

  @RuleProperty(
    key = "headerFormat",
    description = "Expected copyright and license header",
    defaultValue = DEFAULT_HEADER_FORMAT,
    type = "TEXT")
  public String headerFormat = DEFAULT_HEADER_FORMAT;

  @RuleProperty(
    key = "isRegularExpression",
    description = "Whether the headerFormat is a regular expression",
    defaultValue = "false")
  public boolean isRegularExpression = false;

  private String[] expectedLines;
  private Pattern searchPattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.emptyList();
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.context = context;
    if (isRegularExpression) {
      if (searchPattern == null) {
        try {
          searchPattern = Pattern.compile(getHeaderFormat(), Pattern.DOTALL);
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("[" + getClass().getSimpleName() + "] Unable to compile the regular expression: " + headerFormat, e);
        }
      }
    } else {
      expectedLines = headerFormat.split("(?:\r)?\n|\r");
    }
    visitFile();
  }

  private String getHeaderFormat() {
    String format = headerFormat;
    if(format.charAt(0) != '^') {
      format = "^" + format;
    }
    return format;
  }

  private void visitFile() {
    if (isRegularExpression) {
      checkRegularExpression(context.getFileContent());
    } else {
      if (!matches(expectedLines, context.getFileLines())) {
        addIssueOnFile(MESSAGE);
      }
    }
  }

  private void checkRegularExpression(String fileContent) {
    Matcher matcher = searchPattern.matcher(fileContent);
    if (!matcher.find() || matcher.start() != 0) {
      addIssueOnFile(MESSAGE);
    }
  }

  private static boolean matches(String[] expectedLines, List<String> lines) {
    boolean result;

    if (expectedLines.length <= lines.size()) {
      result = true;

      Iterator<String> it = lines.iterator();
      for (int i = 0; i < expectedLines.length; i++) {
        String line = it.next();
        if (!line.equals(expectedLines[i])) {
          result = false;
          break;
        }
      }
    } else {
      result = false;
    }

    return result;
  }

}
