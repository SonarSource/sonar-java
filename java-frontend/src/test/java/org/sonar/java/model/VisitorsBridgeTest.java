/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.ArrayList;
import org.assertj.core.api.Fail;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.Nullable;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class VisitorsBridgeTest {

  @Rule
  public LogTester logTester = new LogTester();

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
  public void log_only_50_elements() throws Exception {
    DecimalFormat formatter = new DecimalFormat("00");
    IntFunction<String> classNotFoundName = i -> "NotFound" + formatter.format(i);
    VisitorsBridge visitorsBridge =
      new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
        assertThat(context.getSemanticModel()).isNotNull();
        ((SemanticModel) context.getSemanticModel()).classesNotFound().addAll(IntStream.range(0, 60).mapToObj(classNotFoundName).collect(Collectors.toList()));
      }), Lists.newArrayList(), null);
    checkFile("Foo.java", "class Foo {}", visitorsBridge);
    visitorsBridge.endOfAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Classes not found during the analysis : ["+
      IntStream.range(0, 50 /*only first 50 missing classes are displayed in the log*/).mapToObj(classNotFoundName).sorted().collect(Collectors.joining(", "))
      +", ...]"
    );
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
      return (CompilationUnitTree) JavaParser.createParser().parse(code);
    } catch (RecognitionException e) {
      return null;
    }
  }

  @Test
  public void rethrow_exception_when_hidden_property_set_to_true() {
    NullPointerException npe = new NullPointerException("BimBadaboum");
    JavaFileScanner visitor = c -> {throw npe;};
    File currentFile = new File("");
    SensorContextTester sensorContextTester = SensorContextTester.create(currentFile);
    SonarComponents sonarComponents = new SonarComponents(null, null, null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);
    VisitorsBridge visitorsBridge = new VisitorsBridge(Collections.singleton(visitor), new ArrayList<>(), sonarComponents);
    visitorsBridge.setCurrentFile(currentFile);
    try {
      visitorsBridge.visitFile(null);
      assertThat(sonarComponents.analysisErrors).hasSize(1);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }

    sensorContextTester.settings().appendProperty("sonar.java.failOnException", "true");
    try {
      visitorsBridge.visitFile(null);
      Fail.fail("scanning of file should have raise an exception");
    } catch (Exception e) {
      assertThat(e).isSameAs(npe);
    }

  }
}
