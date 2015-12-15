/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.model;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
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
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.api.SourceProject;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class InternalVisitorsBridgeTest {

  private final VisitorContext context = new VisitorContext(new SourceProject("Java project"));

  @Test
  public void test_semantic_exclusions() {
    InternalVisitorsBridge visitorsBridgeWithoutSemantic = new InternalVisitorsBridge(Collections.singletonList(new JavaFileScanner() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.getSemanticModel() == null).isTrue();
        assertThat(context.fileParsed()).isTrue();
      }
    }), Lists.<File>newArrayList(), null);
    visitorsBridgeWithoutSemantic.setContext(context);
    checkFile(contstructFileName("java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("src", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("home", "user", "oracleSdk", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "io", "Serializable.java"), "package java.io; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "lang", "annotation", "Annotation.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithoutSemantic);
    InternalVisitorsBridge visitorsBridgeWithSemantic = new InternalVisitorsBridge(Collections.singletonList(new IssuableSubscriptionVisitor() {
      public ClassTree enclosingClass;

      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.METHOD);
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.getSemanticModel()).isNotNull();
        assertThat(context.fileParsed()).isTrue();
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
    }), Lists.<File>newArrayList(), null);
    visitorsBridgeWithSemantic.setContext(context);
    checkFile(contstructFileName("java", "lang", "annotation", "Foo.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithSemantic);
    checkFile(contstructFileName("java", "io", "File.java"), "package java.io; class A {}", visitorsBridgeWithSemantic);
    checkFile(contstructFileName("src", "foo", "bar", "java", "lang", "someFile.java"), "package foo.bar.java.lang; class A { void method() { ; } }", visitorsBridgeWithSemantic);

    InternalVisitorsBridge visitorsBridgeWithParsingIssue = new InternalVisitorsBridge(Collections.singletonList(new IssuableSubscriptionVisitor() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.fileParsed()).isFalse();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return ImmutableList.of(Tree.Kind.METHOD);
      }
    }), Lists.<File>newArrayList(), null);
    visitorsBridgeWithParsingIssue.setContext(context);
    checkFile(contstructFileName("org", "foo", "bar", "Foo.java"), "class Foo { arrrrrrgh", visitorsBridgeWithParsingIssue);
  }

  private void checkFile(String filename, String code, InternalVisitorsBridge visitorsBridge) {
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

  @Nullable
  private static CompilationUnitTree parse(String code) {
    try {
      return (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(code);
    } catch (RecognitionException e) {
      return null;
    }
  }

}
