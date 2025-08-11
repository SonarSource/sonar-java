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
package org.sonar.java.model.expression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class LiteralTreeImplTest {

  @Test
  void parsed_value() throws IOException {
    // begin samples
    Object[] samples = {
      // null
      null,
      // boolean
      true,
      false,
      // int
      0,
      1,
      1_000_000,
      // Integer.MIN_VALUE,
      0x80000000,
      // Integer.MAX_VALUE,
      0x7fffffff,
      // -1
      0xffffffff,
      // long
      42L,
      // float
      1.2f,
      // double
      1.2d,
      // character
      'a',
      // character with escaped character
      '\n',
      '\u26A0',
      // empty string
      "",
      // empty text box
      """
        """,
      // string with escaped characters
      "\"\u26A0a\012cdef\"",
      // string with escaped new lines
      "1\r\n2\r\n3\r\n",
      // text block without indentation
      """
        line 1
        line 2
        """,
      // text block with 2 space indentation
      """
          line 1
          line 2
        """,
      // text block with escaped characters
      """
        \u26A0 line\t1
        \u26A0 line\t2
        """,
      // text block with ending whitespace
      """
          line 3  \s
          line 4  \s
        """,
      // text block without a new line at the end
      """
        line 5  \s
        line 6""",
      // text block with tailing space before the end are trimmed
      """
        line 7  \s
        line 8  """
    };
    // end samples
    String aboveSamplesSourceCode = "class A {\n    " +
      extractJavaSourceCodeFromTheCurrentFileBetween("// begin samples\n", "// end samples\n") +
      "\n}";

    CompilationUnitTree compilationUnit = JParserTestUtils.parse(aboveSamplesSourceCode);
    NewArrayTree samplesArray = (NewArrayTree) ((VariableTree) ((ClassTree) compilationUnit.types().get(0)).members().get(0)).initializer();

    ListTree<ExpressionTree> initializers = samplesArray.initializers();
    for (int i = 0; i < initializers.size(); i++) {
      LiteralTree initializer = (LiteralTree) initializers.get(i);
      assertThat(initializer.parsedValue())
        .describedAs("For string at index " + i + " with token " + initializer.value())
        .isEqualTo(samples[i]);
    }
  }

  @NotNull
  private String extractJavaSourceCodeFromTheCurrentFileBetween(String startTag, String endTag) throws IOException {
    Path thisJavaFilePath = Path.of("src", "test", "java", this.getClass().getName().replace('.', '/').concat(".java"));
    String sourceCode = Files.readString(thisJavaFilePath);
    int samplesStart = sourceCode.indexOf(startTag);
    int samplesEnd = sourceCode.indexOf(endTag);
    if (samplesStart == -1 || samplesEnd == -1) {
      throw new IllegalStateException("Could not find tags in " + thisJavaFilePath);
    }
    return sourceCode.substring(samplesStart, samplesEnd);
  }

}
