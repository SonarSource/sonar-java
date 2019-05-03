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

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "S00103")
@RspecKey("S103")
public class TooLongLineCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAXIMUM_LINE_LENGTH = 120;

  @RuleProperty(
      key = "maximumLineLength",
      description = "The maximum authorized line length.",
      defaultValue = "" + DEFAULT_MAXIMUM_LINE_LENGTH)
  int maximumLineLength = DEFAULT_MAXIMUM_LINE_LENGTH;

  private Set<Integer> ignoredLines = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.emptyList();
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    ignoredLines.clear();
    ignoreLines(context.getTree());
    super.setContext(context);
    visitFile();
  }

  private void ignoreLines(CompilationUnitTree tree) {
    List<ImportClauseTree> imports = tree.imports();
    if (!imports.isEmpty()) {
      int start = getLine(imports.get(0), true);
      int end = getLine(imports.get(imports.size() - 1), false);
      for (int i = start; i <= end; i++) {
        ignoredLines.add(i);
      }
    }
  }

  private static int getLine(ImportClauseTree importClauseTree, boolean fromStart) {
    if (importClauseTree.is(Tree.Kind.IMPORT)) {
      if (fromStart) {
        return ((ImportTree) importClauseTree).importKeyword().line();
      } else {
        return ((ImportTree) importClauseTree).semicolonToken().line();
      }
    }
    return ((EmptyStatementTree) importClauseTree).semicolonToken().line();
  }

  private void visitFile() {
    List<String> lines = context.getFileLines();
    for (int i = 0; i < lines.size(); i++) {
      if (!ignoredLines.contains(i + 1)) {
        String origLine = lines.get(i);
        String line = removeIgnoredPatterns(origLine);
        if (line.length() > maximumLineLength) {
          addIssue(i + 1, MessageFormat.format("Split this {0} characters long line (which is greater than {1} authorized).", origLine.length(), maximumLineLength));
        }
      }
    }
  }

  private static String removeIgnoredPatterns(String line) {
    return line
      // @see <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#link">@link ...</a>
      .replaceAll("^(\\s*(\\*|//).*?)\\s*\\{@link [^}]+\\}\\s*", "$1")
      // @see <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#see">@see reference</a>
      .replaceAll("^(\\s*(\\*|//).*?)\\s*@see .+\\s*", "$1");
  }
}
