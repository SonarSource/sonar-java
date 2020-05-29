/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Collections;
import org.opentest4j.AssertionFailedError;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class RegexParserTestUtils {

  // place the String which will contain the regex on 3rd line, starting from index 0
  private static final String JAVA_CODE = "class Foo {\n  String str = \n\"%s\";\n}";

  public static RegexTree parseRegex(String regex) {
    RegexSource source = makeSource(regex);
    RegexParseResult result = new RegexParser(source).parse();
    if (!result.getSyntaxErrors().isEmpty()) {
      throw new AssertionFailedError("Parsing should complete with no errors.", "no errors", result.getSyntaxErrors());
    }
    return result.getResult();
  }

  private static RegexSource makeSource(String regex) {
    CompilationUnitTree tree = JParserTestUtils.parse(String.format(JAVA_CODE, regex));
    LiteralTree literal = (LiteralTree) ((VariableTree) ((ClassTree) tree.types().get(0)).members().get(0)).initializer();
    return new RegexSource(Collections.singletonList(literal));
  }
}
