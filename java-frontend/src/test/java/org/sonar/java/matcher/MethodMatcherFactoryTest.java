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
package org.sonar.java.matcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MethodMatcherFactoryTest {

  @Test
  public void fail_arg() throws Exception {
    try {
      MethodMatcherFactory.methodMatcher("org.sonar.test.Test$match");
      fail("Argument should not be accepted.");
    } catch (IllegalArgumentException iae) {
      assertThat(iae.getMessage()).contains("method");
    }

    try {
      MethodMatcherFactory.constructorMatcher("   %");
      fail("Argument should not be accepted.");
    } catch (IllegalArgumentException iae) {
      assertThat(iae.getMessage()).contains("constructor");
    }

    try {
      MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match(java.lang.String;int)");
      fail("Argument should not be accepted.");
    } catch (IllegalArgumentException iae) {
      assertThat(iae.getMessage()).contains("constructor").contains("method");
    }

    try {
      MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match(java.lang.String,int)followed by anything");
      fail("Argument should not be accepted.");
    } catch (IllegalArgumentException iae) {
      assertThat(iae.getMessage()).contains("constructor").contains("method");
    }
    try {
      MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match this is an error");
      fail("Argument should not be accepted.");
    } catch (IllegalArgumentException iae) {
      assertThat(iae.getMessage()).contains("method");
    }
  }

  @Test
  public void inner_classes() throws Exception {
    MethodMatcher anyArg = MethodMatcherFactory.methodMatcher("org.sonar.test.Outer$Inner#foo");
    MethodVisitor visitor = new MethodVisitor();
    visitor.add(anyArg);
    scanWithVisitor(visitor, TestUtils.inputFile("src/test/files/matcher/InnerClass.java"));
    assertThat(visitor.count(anyArg)).isEqualTo(1);
  }

  @Test
  public void methodFactoryMatching() {
    MethodMatcher anyArg = MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match");
    MethodMatcher stringOnly = MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match(java.lang.String)");
    MethodMatcher stringInt = MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match(java.lang.String,int)");
    MethodMatcher intInt = MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match(int,int)");
    MethodMatcher onlyBoolean = MethodMatcherFactory.methodMatcher("org.sonar.test.Test#match(java.lang.Boolean)");

    MethodVisitor visitor = new MethodVisitor();
    visitor.add(anyArg);
    visitor.add(stringOnly);
    visitor.add(stringInt);
    visitor.add(intInt);
    visitor.add(onlyBoolean);

    InputFile testFile = buildTestFile(
      "package org.sonar.test;",
      "private class Test {",
      "   private void match(String a) {}",
      "   private void match(String a, int b) {}",
      "   private void match(int a, int b) {}",
      "   private void caller() {",
      "      match(new String());",
      "      match(new String(), 0);",
      "      match(new String(), 1);",
      "      match(0, 1);",
      "      match(1, 2);",
      "      match(3, 5);",
      "   }",
      "}");
    scanWithVisitor(visitor, testFile);

    assertThat(visitor.count(anyArg)).isEqualTo(6);
    assertThat(visitor.count(stringOnly)).isEqualTo(1);
    assertThat(visitor.count(stringInt)).isEqualTo(2);
    assertThat(visitor.count(intInt)).isEqualTo(3);
    assertThat(visitor.count(onlyBoolean)).isEqualTo(0);
  }

  @Test
  public void constructorFactoryMatching() {
    MethodMatcher anyArg = MethodMatcherFactory.constructorMatcher("java.lang.String");
    MethodMatcher noArg = MethodMatcherFactory.constructorMatcher("java.lang.String()");
    MethodMatcher stringBuilder = MethodMatcherFactory.constructorMatcher("java.lang.String(java.lang.StringBuilder)");
    MethodMatcher stringBytes = MethodMatcherFactory.constructorMatcher("java.lang.String(byte[],int,int)");
    MethodVisitor visitor = new MethodVisitor();
    visitor.add(anyArg);
    visitor.add(noArg);
    visitor.add(stringBuilder);
    visitor.add(stringBytes);

    InputFile testFile = buildTestFile(
      "package org.sonar.test;",
      "private class Test {",
      "   private void caller() {",
      "      byte[] bytes = \"Hello world!\".getBytes();",
      "      new String();",
      "      new String(new StringBuilder());",
      "      new String(bytes, 6, 5);",
      "      new String(bytes, 0, 5);",
      "   }",
      "}");
    scanWithVisitor(visitor, testFile);

    assertThat(visitor.count(anyArg)).isEqualTo(4);
    assertThat(visitor.count(noArg)).isEqualTo(1);
    assertThat(visitor.count(stringBuilder)).isEqualTo(1);
    assertThat(visitor.count(stringBytes)).isEqualTo(2);
  }

  private void scanWithVisitor(MethodVisitor visitor, InputFile testFile) {
    JavaAstScanner.scanSingleFileForTests(testFile, new VisitorsBridge(Collections.singletonList(visitor), new ArrayList<>(), null));
  }

  public static InputFile buildTestFile(String... codeLines) {
    try {
      File file = File.createTempFile("InLineTest", ".java");
      file.deleteOnExit();
      try (PrintWriter printer = new PrintWriter(file)) {
        for (String line : codeLines) {
          printer.println(line);
        }
      }
      return TestUtils.inputFile(file);
    } catch (IOException e) {
      Assert.fail("Unable to create inline test file: " + e.getMessage());
      return null;
    }
  }

  private static class MethodVisitor extends SubscriptionVisitor {

    private final Map<MethodMatcher, Integer> matches = new HashMap<>();

    void add(MethodMatcher matcher) {
      matches.put(matcher, Integer.valueOf(0));
    }

    int count(MethodMatcher matcher) {
      // Generates an NPE if the matcher was not registered, but this OK for a test.
      return matches.get(matcher).intValue();
    }

    @Override
    public void visitNode(Tree tree) {
      super.visitNode(tree);
      if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        visitMethodInvocation((MethodInvocationTree) tree);
      } else if (tree.is(Tree.Kind.NEW_CLASS)) {
        visitNewClass((NewClassTree) tree);
      }
    }

    public void visitNewClass(NewClassTree tree) {
      for (MethodMatcher matcher : matches.keySet()) {
        if (matcher.matches(tree)) {
          countMatch(matcher);
        }
      }
    }

    private void visitMethodInvocation(MethodInvocationTree tree) {
      for (MethodMatcher matcher : matches.keySet()) {
        if (matcher.matches(tree)) {
          countMatch(matcher);
        }
      }
    }

    public void countMatch(MethodMatcher matcher) {
      int n = matches.get(matcher).intValue() + 1;
      matches.put(matcher, Integer.valueOf(n));
    }

    @Override
    public List<Kind> nodesToVisit() {
      return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
    }
  }
}
