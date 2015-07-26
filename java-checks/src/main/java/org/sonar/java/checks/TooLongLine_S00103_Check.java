/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Rule(
  key = "S00103",
  name = "Lines should not be too long",
  tags = {"convention"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class TooLongLine_S00103_Check extends SubscriptionBaseVisitor implements CharsetAwareVisitor {

  private static final int DEFAULT_MAXIMUM_LINE_LENHGTH = 120;

  @RuleProperty(
      key = "maximumLineLength",
      description = "The maximum authorized line length.",
      defaultValue = "" + DEFAULT_MAXIMUM_LINE_LENHGTH)
  public int maximumLineLength = DEFAULT_MAXIMUM_LINE_LENHGTH;

  private Charset charset;
  private Set<Integer> ignoredLines = Sets.newHashSet();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.emptyList();
  }

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.context = context;
    ignoredLines.clear();
    ignoreLines(context.getTree());
    super.scanFile(context);
    visitFile(context.getFile());
  }

  public void ignoreLines(CompilationUnitTree tree) {
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

  private void visitFile(File file) {
    List<String> lines;
    try {
      lines = Files.readLines(file, charset);
    } catch (IOException e) {
      throw new SonarException(e);
    }
    for (int i = 0; i < lines.size(); i++) {
      if (!ignoredLines.contains(i + 1)) {
        String line = lines.get(i);
        if (line.length() > maximumLineLength) {
          addIssue(i + 1, MessageFormat.format("Split this {0} characters long line (which is greater than {1} authorized).", line.length(), maximumLineLength));
        }
      }
    }
  }


}
