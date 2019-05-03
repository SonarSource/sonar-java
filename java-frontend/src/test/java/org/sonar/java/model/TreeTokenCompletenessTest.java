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
package org.sonar.java.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TreeTokenCompletenessTest {

  private static final String EOL = System.getProperty("line.separator");

  @Test
  public void test() {
    // test itself
    File file = new File("src/test/java/org/sonar/java/model/TreeTokenCompletenessTest.java");

    List<String> basedOnSyntaxTree = readFileFromSyntaxTree(TestUtils.inputFile(file));
    List<String> basedOnFileLine = readFile(file);
    Map<Integer, String> differences = getDifferences(basedOnSyntaxTree, basedOnFileLine);

    assertThat(basedOnSyntaxTree).isNotEmpty();
    assertThat(basedOnSyntaxTree.size()).isEqualTo(basedOnFileLine.size());

    // printListString(basedOnSyntaxTree);
    // printDifferences(differences);

    // the difference is on parsing on generic: "line 117 : 'Lists.<File>newArrayList(), null));'"
    assertThat(differences).hasSize(1);
  }

  private static Map<Integer, String> getDifferences(List<String> basedOnSyntaxTree, List<String> basedOnFileLine) {
    Map<Integer, String> differences = Maps.newHashMap();
    for (int i = 0; i < basedOnSyntaxTree.size(); i++) {
      String lineFromSyntaxTree = basedOnSyntaxTree.get(i);
      String lineFromFile = basedOnFileLine.get(i);
      if (!StringUtils.isBlank(lineFromSyntaxTree) && !StringUtils.isBlank(lineFromFile)) {
        String difference = StringUtils.difference(lineFromSyntaxTree, lineFromFile);
        if (!difference.isEmpty()) {
          differences.put(i + 1, difference);
        }
      }
    }
    return differences;
  }

  private static void printDifferences(Map<Integer, String> differences) {
    List<Integer> keys = Lists.newArrayList(differences.keySet());
    Collections.sort(keys);

    List<String> diffsWithLines = Lists.newLinkedList();
    for (Integer key : keys) {
      diffsWithLines.add("line " + String.format("%03d", key) + " : '" + differences.get(key) + "'");
    }

    printDiffHeader();
    printListString(diffsWithLines);
  }

  private static void printDiffHeader() {
    StringBuilder builder = new StringBuilder();
    builder.append(EOL);
    builder.append(EOL);
    builder.append("----------------------- diff -------------------------------");
    builder.append(EOL);
    System.out.println(builder.toString());
  }

  private static void printListString(List<String> basedOnSyntaxTree) {
    for (String line : basedOnSyntaxTree) {
      System.out.println(line);
    }
  }

  private static List<String> readFile(File file) {
    try {
      return FileUtils.readLines(file);
    } catch (IOException e) {
      fail("can not read test file");
    }
    return Collections.emptyList();
  }

  private static List<String> readFileFromSyntaxTree(InputFile inputFile) {
    TokenPrinter tokenPrinter = new TokenPrinter();
    JavaAstScanner.scanSingleFileForTests(inputFile, new VisitorsBridge(Collections.singletonList(tokenPrinter), Lists.<File>newArrayList(), null));
    return tokenPrinter.getPrintedFile();
  }

  private static class TokenPrinter extends SubscriptionVisitor {
    private int lastLine = 1;
    private int lastColumn = 0;
    private StringBuilder resultBuilder = new StringBuilder();

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TOKEN);
    }

    @Override
    public void visitToken(SyntaxToken syntaxToken) {
      for (SyntaxTrivia trivia : syntaxToken.trivias()) {
        printTrivia(trivia);
      }
      printToken(syntaxToken);
    }

    private void printToken(SyntaxToken token) {
      int deltaLine = token.line() - lastLine;
      for (int i = 0; i < deltaLine; i++) {
        newLine();
      }
      int deltaColumn = token.column() - lastColumn;
      for (int i = 0; i < deltaColumn; i++) {
        space();
      }
      String text = token.text();
      print(text);
      lastColumn += text.length();
    }

    private void printTrivia(SyntaxTrivia trivia) {
      String comment = trivia.comment();
      int deltaLine = trivia.startLine() - lastLine;
      for (int i = 0; i < deltaLine; i++) {
        newLine();
      }
      int numberEOL = StringUtils.countMatches(comment, EOL);
      if (numberEOL > 0) {
        lastLine = trivia.startLine() + numberEOL; // recalculate the last line
      }
      int deltaColumn = trivia.column() - lastColumn;
      for (int i = 0; i < deltaColumn; i++) {
        space();
      }
      print(comment);

      if (numberEOL > 0) {
        lastColumn = StringUtils.substringAfterLast(comment, EOL).length();
      } else {
        lastColumn += comment.length();
      }
    }

    private void print(String text) {
      resultBuilder.append(text);
    }

    private void newLine() {
      lastColumn = 0;
      lastLine++;
      resultBuilder.append(System.getProperty("line.separator"));
    }

    public void space() {
      lastColumn++;
      resultBuilder.append(" ");
    }

    public List<String> getPrintedFile() {
      return Lists.newArrayList(resultBuilder.toString().split(EOL));
    }
  }
}
