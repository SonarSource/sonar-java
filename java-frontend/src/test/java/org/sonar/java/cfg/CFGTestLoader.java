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
package org.sonar.java.cfg;

import com.sonar.sslr.api.typed.ActionParser;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CFGTestLoader {

  private CompilationUnitTree compiledTest;

  public CFGTestLoader(String fileName) {
    final File file = new File(fileName);
    try (StringWriter buffer = new StringWriter(); PrintWriter printer = new PrintWriter(buffer);) {
      List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
      for (String line : lines) {
        printer.println(line);
      }
      printer.flush();
      ActionParser<Tree> parser = JavaParser.createParser();
      compiledTest = (CompilationUnitTree) parser.parse(buffer.toString());
    } catch (Exception e) {
      Assert.fail("Unable to compile file " + file.getAbsolutePath());
    }
  }

  public MethodTree getMethod(String className, String methodName) {
    for (Tree type : compiledTest.types()) {
      if (type.is(Tree.Kind.CLASS)) {
        ClassTree classTree = (ClassTree) type;
        if (className.equals(classTree.simpleName().name())) {
          for (Tree member : classTree.members()) {
            if (member.is(Tree.Kind.METHOD)) {
              MethodTree method = (MethodTree) member;
              if (methodName.equals(method.simpleName().name())) {
                return method;
              }
            }
          }
        }
      }
    }
    Assert.fail("Method " + methodName + " of class " + className + " not found!");
    return null;
  }
}
