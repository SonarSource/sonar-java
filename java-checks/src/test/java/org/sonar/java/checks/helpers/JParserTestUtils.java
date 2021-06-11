/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.helpers;

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class JParserTestUtils {

  static IdentifierTree variableFromLastReturnStatement(List<StatementTree> statements) {
    return (IdentifierTree) ((ReturnStatementTree) statements.get(statements.size() - 1)).expression();
  }

  static ExpressionTree assignementExpressionFromStatement(StatementTree statement) {
    return ((AssignmentExpressionTree) ((ExpressionStatementTree) statement).expression()).expression();
  }

  static ExpressionTree initializerFromVariableDeclarationStatement(Tree statement) {
    return ((VariableTree) statement).initializer();
  }

  public static ClassTree classTree(String classBody) {
    CompilationUnitTree compilationUnitTree = parse(classBody);
    return (ClassTree) compilationUnitTree.types().get(0);
  }

  public static MethodTree methodTree(String classBody) {
    ClassTree firstType = classTree(classBody);
    return (MethodTree) firstType.members().get(0);
  }

  public static List<StatementTree> methodBody(String code) {
    return methodTree(code).block().body();
  }

  public static String newCode(String... lines) {
    String lineSeparator = System.lineSeparator();
    StringBuilder sb = new StringBuilder("class A {").append(lineSeparator);
    for (String string : lines) {
      sb.append(string).append(lineSeparator);
    }
    return sb.append("}").append(lineSeparator).toString();
  }

  public static CompilationUnitTree parse(File file) {
    try {
      return parse(Files.readLines(file, StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n")));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read file", e);
    }
  }

  public static CompilationUnitTree parse(String source) {
    List<File> classpath = Arrays.asList(new File("target/test-classes"), new File("target/classes"));
    String version = JParser.MAXIMUM_SUPPORTED_JAVA_VERSION;
    return JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(version, classpath).astParser(), version, "test", source);
  }

}
