/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api.query;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.charset.StandardCharsets.UTF_8;

public class QueryViewerUtils {

  private static final Pattern SOURCE_STRING_LITERAL = Pattern.compile("(?<prefix>\n +var source = \"\"\"\n)" +
    "(?:[^\"]|\"[^|\"]|\"\"[^\"])*\n(?<indent> +)(?<suffix>\"\"\";)");

  private static final Pattern ISSUE_PATTERN = Pattern.compile("(?:^|\n) *+\\^[^\\n]*(\n|$)");

  public record Issue(Tree tree, String message) {
  }

  public record TestContext(List<Issue> issues) {
    public void reportIssue(Tree tree, String message) {
      issues.add(new Issue(tree, message));
    }
  }

  public static String removeIssues(String source) {
    return ISSUE_PATTERN.matcher(source).replaceAll("$1");
  }

  public static void replaceTestSourceCode(Class<?> tagerClass, String newContent) throws IOException {
    Path sourceFile = javaFrontEndPath()
      .resolve(Path.of("src", "test", "java"))
      .resolve(tagerClass.getName().replace('.', File.separatorChar) + ".java");
    var code = Files.readString(sourceFile, UTF_8);
    var matcher = SOURCE_STRING_LITERAL.matcher(code);
    if (!matcher.find()) {
      throw new IllegalStateException("Source code not found!");
    }
    int indent = matcher.group("indent").length();
    String newCode = code.substring(0, matcher.start()) +
      matcher.group("prefix") +
      newContent.indent(indent) +
      matcher.group("indent") +
      matcher.group("suffix") +
      code.substring(matcher.end(), code.length());
    Files.writeString(sourceFile, newCode, UTF_8);
  }

  public static Path javaFrontEndPath() {
    var currentDir = Paths.get(".").toAbsolutePath().normalize();
    return Stream.of(currentDir, currentDir.resolve("java-frontend"))
      .filter(Files::exists)
      .filter(p -> p.getFileName().toString().equals("java-frontend"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No java-frontend directory found"));
  }

  public static String applyQueryOnSourceCodeAndUpdateTheIssues(
    Selector<TestContext, ? extends Tree> query,
    String source,
    Class<?> targetClassToUpdateWithNewSource) throws IOException {
    var compilationUnit = JParserTestUtils.parse(removeIssues(source));
    var ctx = new TestContext(new ArrayList<>());
    query.apply(ctx, compilationUnit);
    var sourceWithIssuesFromQuery = renderExpectedSource(removeIssues(source), ctx.issues());
    replaceTestSourceCode(targetClassToUpdateWithNewSource, sourceWithIssuesFromQuery);
    return sourceWithIssuesFromQuery;
  }

  public static String renderExpectedSource(String source, List<Issue> collectedIssues) {
    StringBuilder out = new StringBuilder();
    var lines = source.split("\n");
    for (int i = 0; i < lines.length; i++) {
      out.append(lines[i]).append("\n");
      printIssuesAtLine(out, i + 1, lines[i].length(), collectedIssues);
    }
    return out.toString();
  }

  private static void printIssuesAtLine(StringBuilder out, int line, int length, List<Issue> collectedIssues) {
    for (Issue issue : collectedIssues) {
      var start = issue.tree.firstToken().range().start();
      var end = issue.tree.lastToken().range().end();
      if (start.line() == line && end.line() == line) {
        out.append(" ".repeat(start.column() - 1));
        out.append("^".repeat(end.column() - start.column()));
        out.append(" {").append(issue.message()).append("}\n");
      } else if (start.line() == line) {
        out.append(" ".repeat(start.column() - 1));
        out.append("^".repeat(length - start.column()));
        out.append(" {").append(issue.message()).append("...\n");
      } else if (end.line() == line) {
        out.append("^".repeat(end.column() - 1));
        out.append(" ...").append(issue.message()).append("}\n");
      }
    }
  }

}
