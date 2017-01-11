/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.io.Files;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.AnalysisException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(key = "S1451")
public class FileHeaderCheck extends IssuableSubscriptionVisitor implements CharsetAwareVisitor {

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

  private Charset charset;
  private String[] expectedLines;
  private Pattern searchPattern = null;

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.emptyList();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.context = context;
    if (isRegularExpression) {
      if (searchPattern == null) {
        try {
          searchPattern = Pattern.compile(headerFormat, Pattern.DOTALL);
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("[" + getClass().getSimpleName() + "] Unable to compile the regular expression: " + headerFormat, e);
        }
      }
    } else {
      expectedLines = headerFormat.split("(?:\r)?\n|\r");
    }
    visitFile(context.getFile());
  }

  public void visitFile(File file) {
    if (isRegularExpression) {
      String fileContent;
      try {
        fileContent = Files.toString(file, charset);
      } catch (IOException e) {
        throw new AnalysisException(e);
      }
      checkRegularExpression(fileContent);
    } else {
      List<String> lines;
      try {
        lines = Files.readLines(file, charset);
      } catch (IOException e) {
        throw new AnalysisException(e);
      }
      if (!matches(expectedLines, lines)) {
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
