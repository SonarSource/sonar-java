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
package org.sonar.java.checks.eclipsebug;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EclipseBugTest {

  /**
   * Should be executed with java version > 9.
   *
   * Apparently fixed with version 3.19 of ECJ, which we can not migrate yet, as long as following bug is not fixed:
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=551426
   */
  @Test
  public void javax_conflict() {
    assertThrows(IllegalStateException.class, () -> run());
  }

  private void run() {
    // contains a "javax" package, but any empty directory containing a "javax" empty folder would do it
    List<File> classPath = Collections.singletonList(new File("target/test-jars/jsr305-1.3.9.jar"));
    for (int i = 0; i < 1000; i++) {
      i++;
      System.out.println("Iteration " + i);
      String source = "" +
        "class Example {\n" +
        "  void example() {\n" +
        "    System.out.println(javax.naming.Context.class);\n" + // sometimes leads to "The type javax.naming.Context is not accessible"
        "  }\n" +
        "}\n";

      ASTParser astParser = ASTParser.newParser(AST.JLS12);

      Map<String, String> options = new HashMap<>(JavaCore.getDefaultOptions());
      String version = "12";
      options.put(JavaCore.COMPILER_COMPLIANCE, version);
      options.put(JavaCore.COMPILER_SOURCE, version);
      options.put(JavaCore.COMPILER_RELEASE, version);
      options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
      astParser.setCompilerOptions(options);

      astParser.setEnvironment(
        classPath.stream().map(File::getAbsolutePath).toArray(String[]::new),
        new String[] {},
        new String[] {},
        true);
      astParser.setUnitName("Example.java");
      astParser.setResolveBindings(true);
      astParser.setBindingsRecovery(true);

      char[] sourceChars = source.toCharArray();
      astParser.setSource(sourceChars);

      CompilationUnit astNode = (CompilationUnit) astParser.createAST(null);

      IProblem[] problems = astNode.getProblems();
      if (problems.length > 0) {
        String message0 = problems[0].getMessage();
        System.err.println(message0);
        throw new IllegalStateException(message0);
      }
    }
  }

}
