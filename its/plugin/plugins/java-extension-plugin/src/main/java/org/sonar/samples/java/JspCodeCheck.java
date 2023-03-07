/*
 * SonarQube Java
 * Copyright (C) 2013-2023 SonarSource SA
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
package org.sonar.samples.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JspCodeVisitor;
import org.sonar.plugins.java.api.SourceMap;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

@Rule(key = "jspcheck", priority = Priority.MINOR, name = "JspCodeCheck", description = "JspCodeCheck")
public class JspCodeCheck extends IssuableSubscriptionVisitor implements JspCodeVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      visitClass((ClassTree) tree);
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      visitMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private void visitClass(ClassTree tree) {
    Path visitedClasses = context.getWorkingDirectory().toPath().resolve("visit.txt");
    Symbol.TypeSymbol symbol = tree.symbol();
    String name = symbol.type().fullyQualifiedName();
    String parent = symbol.superClass().isUnknown() ? "unknown" : symbol.superClass().fullyQualifiedName();
    try {
      Files.write(visitedClasses, format("%s extends %s%n", name, parent).getBytes(), APPEND, CREATE);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void visitMethodInvocation(MethodInvocationTree tree) {
    if (isInvocation(tree, "index_jsp.java", 116) ||
      isInvocation(tree, "test_005finclude_jsp.java", 124) ||
      isInvocation(tree, "test_005finclude_jsp.java", 129)) {
      context.sourceMap()
        .flatMap(sourceMap -> sourceMap.sourceMapLocationFor(tree))
        .ifPresent(this::writeToFile);
    }
  }

  private boolean isInvocation(MethodInvocationTree tree, String inputFile, int line) {
    return context.getInputFile().filename().equals(inputFile) && tree.firstToken().range().start().line() == line;
  }

  private void writeToFile(SourceMap.Location location) {
    try {
      Path path = context.getWorkingDirectory().toPath().resolve("JspCodeCheck.txt");
      String data = format("%s %d:%d%n", location.file().filename(), location.startLine(), location.endLine());
      Files.write(path, data.getBytes(), APPEND, CREATE);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
