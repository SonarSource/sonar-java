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
package org.sonar.java;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParser;
import org.sonar.java.se.NullableAnnotationUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

public class EcjBug {

  // Does not fail with Java 8
  // Fails with Java 11
  @Test
  void reproducer() {
    List<File> classpath = Arrays
      .asList(
        "src/test/lib/guava-25.1-android.jar",
        "src/test/lib/affinity-3.2.2.jar"
      ).stream()
      .map(File::new)
      .collect(Collectors.toList());

    String source = "public class C {\n" +
      "\n" +
      "    public void plus(java.io.File file) {\n" +
      "        com.google.common.io.Files.createParentDirs(file);\n" +
      "        String problematicString = \"SomeString\";\n" +
      "    }\n" +
      "}\n" +
      "";

    CompilationUnitTree cu = JParser.parse(JParser.MAXIMUM_SUPPORTED_JAVA_VERSION, "test", source, classpath);
    ClassTree main = (ClassTree) cu.types().get(0);
    MethodTree method = (MethodTree) main.members().get(0);
    MethodInvocationTree mit = (MethodInvocationTree) ((ExpressionStatementTree) method.block().body().get(0)).expression();

    assertThat(mit.symbol().isUnknown()).isFalse();
    assertThat(mit.symbol().isMethodSymbol()).isTrue();
    NullableAnnotationUtils.nonNullAnnotationOnParameters((Symbol.MethodSymbol) mit.symbol());
  }
}
