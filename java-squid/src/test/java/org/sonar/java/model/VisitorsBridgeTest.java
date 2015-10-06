/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.SourceProject;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class VisitorsBridgeTest {

  private final VisitorContext context = new VisitorContext(new SourceProject("Java project"));

  @Test
  public void test_semantic_exclusions() {
    VisitorsBridge visitorsBridgeWithoutSemantic = new VisitorsBridge(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.getSemanticModel() == null).isTrue();
      }
    });
    visitorsBridgeWithoutSemantic.setContext(context);
    checkFile(contstructFileName("java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("src", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("home", "user", "oracleSdk", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "io", "Serializable.java"), "package java.io; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "lang", "annotation", "Annotation.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithoutSemantic);
    VisitorsBridge visitorsBridgeWithSemantic = new VisitorsBridge(new IssuableSubscriptionVisitor() {
      public ClassTree enclosingClass;

      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.METHOD);
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.getSemanticModel()).isNotNull();
        super.scanFile(context);
      }

      @Override
      public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.CLASS)) {
          enclosingClass = (ClassTree) tree;
          assertThat(context.getComplexityNodes(enclosingClass).size()).isEqualTo(context.getComplexity(enclosingClass));
        } else {
          assertThat(context.getMethodComplexityNodes(enclosingClass, ((MethodTree) tree)).size()).isEqualTo(context.getMethodComplexity(enclosingClass, ((MethodTree) tree)));
        }
      }
    });
    visitorsBridgeWithSemantic.setContext(context);
    checkFile(contstructFileName("java", "lang", "annotation", "Foo.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithSemantic);
    checkFile(contstructFileName("java", "io", "File.java"), "package java.io; class A {}", visitorsBridgeWithSemantic);
    checkFile(contstructFileName("src", "foo", "bar", "java", "lang", "someFile.java"), "package foo.bar.java.lang; class A { void method() { ; } }", visitorsBridgeWithSemantic);
  }

  private void checkFile(String filename, String code, VisitorsBridge visitorsBridge) {
    context.setFile(new File(filename));
    visitorsBridge.visitFile(parse(code));
  }

  private static String contstructFileName(String... path) {
    String result = "";
    for (String s : path) {
      result += s + File.separator;
    }
    return result.substring(0, result.length() - 1);
  }

  private static CompilationUnitTree parse(String code) {
    return (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(code);
  }

}
