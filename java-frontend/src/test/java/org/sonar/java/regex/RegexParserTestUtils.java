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
package org.sonar.java.regex;

import java.util.ArrayList;
import java.util.List;
import org.opentest4j.AssertionFailedError;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.RegexSource;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegexParserTestUtils {

  private RegexParserTestUtils() {}

  public static RegexTree assertSuccessfulParse(String regex) {
    RegexSource source = makeSource(regex);
    RegexParseResult result = new RegexParser(source, new FlagSet()).parse();
    if (!result.getSyntaxErrors().isEmpty()) {
      throw new AssertionFailedError("Parsing should complete with no errors.", "no errors", result.getSyntaxErrors());
    }
    return result.getResult();
  }

  public static void assertKind(RegexTree.Kind expected, RegexTree actual) {
    assertEquals(expected, actual.kind(), "Regex should have kind " + expected);
    assertTrue(actual.is(expected), "`is` should return true when the kinds match.");
    assertTrue(actual.is(RegexTree.Kind.CHARACTER, RegexTree.Kind.DISJUNCTION, expected), "`is` should return true when one of the kinds match.");
  }

  // place the String which will contain the regex on 3rd line, starting from index 0
  private static final String JAVA_CODE = "class Foo {\n  String str = \n\"%s\";\n}";

  public static RegexSource makeSource(String content) {
    CompilationUnitTree tree = JParserTestUtils.parse(String.format(JAVA_CODE, content));
    ClassTree foo = (ClassTree) tree.types().get(0);
    VariableTree str = (VariableTree) foo.members().get(0);
    LiteralCollector visitor = new LiteralCollector();
    str.initializer().accept(visitor);
    return new JavaAnalyzerRegexSource(visitor.stringLiterals);
  }

  private static class LiteralCollector extends BaseTreeVisitor {

    private final List<LiteralTree> stringLiterals = new ArrayList<>();

    @Override
    public void visitLiteral(LiteralTree tree) {
      if (tree.is(Tree.Kind.STRING_LITERAL, Tree.Kind.TEXT_BLOCK)) {
        stringLiterals.add(tree);
      }
    }
  }
}
