/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1659")
public class OneDeclarationPerLineCheck extends ExtendedIssueBuilderSubscriptionVisitor {

  private static final Pattern INDENTATION_PATTERN = Pattern.compile("^(\\s+)");

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.INTERFACE, Kind.CLASS, Kind.ENUM, Kind.ANNOTATION_TYPE, Kind.BLOCK, Kind.STATIC_INITIALIZER, Kind.CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.INTERFACE, Kind.CLASS, Kind.ENUM, Kind.ANNOTATION_TYPE)) {
      // Field class declaration
      checkVariables(((ClassTree) tree).members());
    } else if (tree.is(Kind.BLOCK, Kind.STATIC_INITIALIZER)) {
      // Local variable declaration (in method, static initialization, ...)
      checkVariables(((BlockTree) tree).body());
    } else if (tree.is(Kind.CASE_GROUP)) {
      checkVariables(((CaseGroupTree) tree).body());
    }
  }

  private void checkVariables(List<? extends Tree> trees) {
    boolean varSameDeclaration = false;
    int lastVarLine = -1;
    List<VariableTree> nodesToReport = new ArrayList<>();

    for (Tree tree : trees) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree varTree = (VariableTree) tree;
        int line = varTree.simpleName().identifierToken().range().start().line();
        if (varSameDeclaration || lastVarLine == line) {
          nodesToReport.add(varTree);
        } else {
          reportIfIssue(nodesToReport);
        }
        varSameDeclaration = ",".equals(varTree.endToken().text());
        lastVarLine = line;
      }
    }
    reportIfIssue(nodesToReport);
  }

  private void reportIfIssue(List<VariableTree> nodesToReport) {
    if (!nodesToReport.isEmpty()) {
      IdentifierTree firstLocation = nodesToReport.get(0).simpleName();
      String moreThanOneMessage = nodesToReport.size() > 1 ? " and all following declarations" : "";
      newIssue()
        .onTree(firstLocation)
        .withMessage("Declare \"%s\"%s on a separate line.", firstLocation.name(), moreThanOneMessage)
        .withSecondaries(nodesToReport.stream().skip(1).map(lit -> new JavaFileScannerContext.Location("", lit.simpleName())).collect(Collectors.toList()))
        .withQuickFix(() -> getQuickFixes(nodesToReport))
        .report();

      nodesToReport.clear();
    }
  }

  private JavaQuickFix getQuickFixes(List<VariableTree> nodesToReport) {
    List<JavaTextEdit> edits = new ArrayList<>();
    SyntaxToken previousToken = QuickFixHelper.previousToken(nodesToReport.get(0));

    for (VariableTree variableTree : nodesToReport) {
      String indentationOfLine = indentationOfLine(variableTree);
      edits.add(getEditForVariable(variableTree, previousToken, indentationOfLine));
      previousToken = variableTree.lastToken();
    }

    return JavaQuickFix.newQuickFix("Declare on separated lines")
      .addTextEdits(edits)
      .build();
  }

  private JavaTextEdit getEditForVariable(VariableTree variableTree, SyntaxToken previousToken, String indentationOfLine) {
    if (",".equals(previousToken.text())) {
      return JavaTextEdit.replaceTextSpan(textSpanBetween(previousToken, true, variableTree.simpleName(), false),
        String.format(";\n%s%s ", indentationOfLine, modifiersAndType(variableTree)));
    } else {
      return JavaTextEdit.replaceTextSpan(textSpanBetween(previousToken, false, variableTree, false),
        String.format("\n%s", indentationOfLine));
    }
  }

  private String modifiersAndType(VariableTree variableTree) {
    ModifiersTree modifiers = variableTree.modifiers();
    if (modifiers.isEmpty()) {
      return QuickFixHelper.contentForTree(variableTree.type(), context);
    }
    return QuickFixHelper.contentForRange(variableTree.modifiers().firstToken(), variableTree.type().lastToken(), context);
  }

  private String indentationOfLine(Tree tree) {
    SyntaxToken firstToken = tree.firstToken();
    Matcher matcher = INDENTATION_PATTERN.matcher(context.getFileLines()
      .get(firstToken.range().start().line() - 1));
    if (matcher.find()) {
      return matcher.group();
    }
    return "";
  }
}
