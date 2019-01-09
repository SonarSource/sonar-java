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
package org.sonar.java.ast.parser.grammar.units;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class CompilationUnitTest {

  @Test
  public void realLife() {
    assertThat(JavaLexer.COMPILATION_UNIT)
      // Ordinary Compilation Unit
      .matches(lines(
        "package org.example;",
        "",
        "public class HelloWorld {",
        "  public static void main(String[] args) {",
        "    System.out.println(\"Hello World!\");",
        "  }",
        "}"))
      // Modular Compilation Unit
      .matches(lines(
        "import com.google.common.annotations.Beta;",
        "",
        "@Beta",
        "open module com.greetings {",
        "  requires transitive org.foo.bar;",
        "  exports foo.bar to com.module1, gul.bar.qix;",
        "  opens gul.lom to moc.loe.module2, ahah.bro.force;",
        "  uses bar.foo.MyInterface;",
        "  provides com.Greetings with org.foo.Greetings, foo.bar.Salutations;",
        "}"));
  }

  private static String lines(String... lines) {
    return Joiner.on("\n").join(lines);
  }

}
