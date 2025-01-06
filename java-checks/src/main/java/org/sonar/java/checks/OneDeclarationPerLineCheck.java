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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.LineUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1659")
public class OneDeclarationPerLineCheck extends IssuableSubscriptionVisitor {

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
        int line = LineUtils.startLine(varTree.simpleName().identifierToken());
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
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(firstLocation)
        .withMessage("Declare \"%s\"%s on a separate line.", firstLocation.name(), moreThanOneMessage)
        .withSecondaries(nodesToReport.stream().skip(1).map(lit -> new JavaFileScannerContext.Location("", lit.simpleName())).toList())
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
      boolean isTypeFragmented = isTypeFragmented(variableTree);
      Tree endTree = isTypeFragmented ? variableTree.type().lastToken() : variableTree.simpleName();
      return JavaTextEdit.replaceTextSpan(
        textSpanBetween(previousToken, true, endTree, isTypeFragmented),
        String.format(";\n%s%s ", indentationOfLine, modifiersAndType(variableTree, isTypeFragmented)));
    } else {
      return JavaTextEdit.replaceTextSpan(textSpanBetween(previousToken, false, variableTree, false),
        String.format("\n%s", indentationOfLine));
    }
  }

  private String modifiersAndType(VariableTree variableTree, boolean isTypeFragmented) {
    StringBuilder sb = new StringBuilder();
    if (!variableTree.modifiers().isEmpty()) {
      sb.append(QuickFixHelper.contentOfTreeTokens(variableTree.modifiers(), context));
      sb.append(" ");
    }
    sb.append(QuickFixHelper.contentOfTreeTokens(variableTree.type(), context));
    sb.append(" ");
    if (isTypeFragmented) {
      sb.append(variableTree.simpleName().name());
    }
    return sb.toString();
  }

  private static boolean isTypeFragmented(VariableTree variableTree) {
    var lastToken = variableTree.type().lastToken();
    return lastToken.range().end().isAfter(variableTree.simpleName().identifierToken().range().start());
  }

  private String indentationOfLine(Tree tree) {
    Matcher matcher = INDENTATION_PATTERN.matcher(context.getFileLines()
      .get(LineUtils.startLine(tree) - 1));
    if (matcher.find()) {
      return matcher.group();
    }
    return "";
  }
}
