/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class TreeTokenCompletenessTest {

  @Test
  void test() throws IOException {
    Path pathOfThisFile = Paths.get("src", "test", "java", "org", "sonar", "java", "model", "TreeTokenCompletenessTest.java");
    String thisFileContent = new String(Files.readAllBytes(pathOfThisFile), UTF_8).replaceAll("\\R", "\n");
    String thisFileContentFromSyntaxTree = readFileFromSyntaxTree(TestUtils.inputFile(pathOfThisFile.toFile()));
    assertThat(thisFileContentFromSyntaxTree)
      .isEqualTo(thisFileContent);
  }

  private static String readFileFromSyntaxTree(InputFile inputFile) {
    TokenPrinter tokenPrinter = new TokenPrinter();
    JavaAstScanner.scanSingleFileForTests(inputFile, new VisitorsBridge(Collections.singletonList(tokenPrinter), Collections.emptyList(), null));
    return tokenPrinter.toString();
  }

  private static class TokenPrinter extends SubscriptionVisitor {
    private Position lastEndPosition = Position.at(Position.FIRST_LINE, Position.FIRST_LINE);
    private final StringBuilder resultBuilder = new StringBuilder();

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TOKEN);
    }

    @Override
    public void visitToken(SyntaxToken syntaxToken) {
      for (SyntaxTrivia trivia : syntaxToken.trivias()) {
        print(trivia.range(), trivia.comment());
      }
      print(syntaxToken.range(), syntaxToken.text());
    }

    private void print(Range range, String text) {
      if (range.start().isBefore(lastEndPosition)) {
        throw new IllegalStateException("Can't write the range: " + range +
          " because the last position is already after at " + lastEndPosition + " for token: " + text);
      }
      int numberOfNewLineToAdd = range.start().line() - lastEndPosition.line();
      resultBuilder.append(StringUtils.repeat("\n", numberOfNewLineToAdd));

      int newLastColumn = (numberOfNewLineToAdd == 0 ? lastEndPosition.column() : Position.FIRST_COLUMN);
      int numberOfSpaceToAdd = range.start().column() - newLastColumn;
      resultBuilder.append(StringUtils.repeat(" ", numberOfSpaceToAdd));

      resultBuilder.append(text);
      lastEndPosition = range.end();
    }

    @Override
    public String toString() {
      return resultBuilder.toString();
    }

  }
}
