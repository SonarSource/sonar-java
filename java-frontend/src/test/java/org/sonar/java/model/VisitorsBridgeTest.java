/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VisitorsBridgeTest {

  @Test
  public void test_semantic_exclusions() {
    VisitorsBridge visitorsBridgeWithoutSemantic = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNull();
      assertThat(context.fileParsed()).isTrue();
    }), Lists.newArrayList(), null);
    checkFile(contstructFileName("java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("src", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("home", "user", "oracleSdk", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "io", "Serializable.java"), "package java.io; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(contstructFileName("java", "lang", "annotation", "Annotation.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithoutSemantic);

    VisitorsBridge visitorsBridgeWithParsingIssue = new VisitorsBridge(Collections.singletonList(new IssuableSubscriptionVisitor() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.fileParsed()).isFalse();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return ImmutableList.of(Tree.Kind.METHOD);
      }
    }), Lists.newArrayList(), null);
    checkFile(contstructFileName("org", "foo", "bar", "Foo.java"), "class Foo { arrrrrrgh", visitorsBridgeWithParsingIssue);
  }

  private void checkFile(String filename, String code, VisitorsBridge visitorsBridge) {
    visitorsBridge.setCurrentFile(new File(filename));
    visitorsBridge.visitFile(parse(code));
  }

  @Test
  public void parameterized_type_should_not_hold_reference() throws Exception {
    VisitorsBridge visitorsBridge =
      new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
        assertThat(context.getSemanticModel()).isNotNull();
        assertThat(ParametrizedTypeJavaType.typeSubstitutionSolver).isNotNull();
      }), Lists.newArrayList(), null);
    checkFile("Foo.java", "class Foo {}", visitorsBridge);
    assertThat(ParametrizedTypeJavaType.typeSubstitutionSolver).isNull();
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
      return (CompilationUnitTree) JavaParser.createParser(StandardCharsets.UTF_8).parse(code);
    } catch (RecognitionException e) {
      return null;
    }
  }

}
