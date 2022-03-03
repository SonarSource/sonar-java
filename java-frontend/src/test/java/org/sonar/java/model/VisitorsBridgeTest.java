/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.CheckFailureException;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.checks.EndOfAnalysisVisitor;
import org.sonar.java.checks.VisitorThatCanSkip;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class VisitorsBridgeTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private SonarComponents sonarComponents = null;

  private static final File FILE = new File("src/test/files/model/SimpleClass.java");
  private static final InputFile INPUT_FILE = TestUtils.inputFile(FILE);
  private static final CompilationUnitTree COMPILATION_UNIT_TREE = JParserTestUtils.parse(FILE);

  private static final NullPointerException NPE = new NullPointerException("BimBadaboum");

  @Test
  @Disabled("Unable to reproduce since ECJ migration")
  void test_semantic_exclusions() {
    VisitorsBridge visitorsBridgeWithoutSemantic = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNull();
      assertThat(context.fileParsed()).isTrue();
    }), new ArrayList<>(), null);
    checkFile(constructFileName("java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("src", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("home", "user", "oracleSdk", "java", "lang", "someFile.java"), "package java.lang; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("java", "io", "Serializable.java"), "package java.io; class A {}", visitorsBridgeWithoutSemantic);
    checkFile(constructFileName("java", "lang", "annotation", "Annotation.java"), "package java.lang.annotation; class Annotation {}", visitorsBridgeWithoutSemantic);

    VisitorsBridge visitorsBridgeWithParsingIssue = new VisitorsBridge(Collections.singletonList(new IssuableSubscriptionVisitor() {
      @Override
      public void scanFile(JavaFileScannerContext context) {
        assertThat(context.fileParsed()).isFalse();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.METHOD);
      }
    }), new ArrayList<>(), null);
    checkFile(constructFileName("org", "foo", "bar", "Foo.java"), "class Foo { arrrrrrgh", visitorsBridgeWithParsingIssue);
  }

  private static void checkFile(String filename, String code, VisitorsBridge visitorsBridge) {
    visitorsBridge.setCurrentFile(TestUtils.emptyInputFile(filename));
    visitorsBridge.visitFile(JParserTestUtils.parse(code), false);
  }


  private static String constructFileName(String... path) {
    String result = "";
    for (String s : path) {
      result += s + File.separator;
    }
    return result.substring(0, result.length() - 1);
  }

  @Test
  void rethrow_exception_when_hidden_property_set_to_true_with_JavaFileScanner() {
    VisitorsBridge visitorsBridge = visitorsBridge(new JFS_ThrowingNPEJavaFileScanner(), true);
    try {
      visitorsBridge.visitFile(COMPILATION_UNIT_TREE, false);
      Fail.fail("scanning of file should have raise an exception");
    } catch (AnalysisException e) {
      assertThat(e.getMessage()).contains("Failing check");
      assertThat(e.getCause()).isInstanceOf(CheckFailureException.class);
      assertThat(e.getCause().getCause()).isSameAs(NPE);
    } catch (Exception e) {
      Fail.fail("Should have been an AnalysisException");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("JFS_ThrowingNPEJavaFileScanner - JFS");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_JavaFileScanner() {
    try {
      visitorsBridge(new JFS_ThrowingNPEJavaFileScanner(), false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("JFS_ThrowingNPEJavaFileScanner - JFS");
  }

  @Test
  void rethrow_exception_when_hidden_property_set_to_true_with_SubscriptionVisitor() {
    VisitorsBridge visitorsBridge = visitorsBridge(new SV1_ThrowingNPEVisitingClass(), true);
    try {
      visitorsBridge.visitFile(COMPILATION_UNIT_TREE, false);
      Fail.fail("scanning of file should have raise an exception");
    } catch (AnalysisException e) {
      assertThat(e.getMessage()).contains("Failing check");
      assertThat(e.getCause()).isInstanceOf(CheckFailureException.class);
      assertThat(e.getCause().getCause()).isSameAs(NPE);
    } catch (Exception e) {
      Fail.fail("Should have been an AnalysisException");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("SV1_ThrowingNPEVisitingClass - SV1");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_SubscriptionVisitor() {
    try {
      visitorsBridge(Arrays.asList(
        new SV1_ThrowingNPEVisitingClass(),
        new SV2_ThrowingNPELeavingClass(),
        new SV3_ThrowingNPEVisitingToken(),
        new SV4_ThrowingNPEVisitingTrivia()),
        false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(4);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder(
        "SV1_ThrowingNPEVisitingClass - SV1",
        "SV2_ThrowingNPELeavingClass - SV2",
        "SV3_ThrowingNPEVisitingToken - SV3",
        "SV4_ThrowingNPEVisitingTrivia - SV4");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_IssuableSubscriptionVisitor() {
    try {
      visitorsBridge(Arrays.asList(
        new IV1_ThrowingNPEVisitingClass(),
        new IV2_ThrowingNPELeavingClass()),
        false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsOnly("IV1_ThrowingNPEVisitingClass - IV1");
  }

  @Test
  void swallow_exception_when_hidden_property_set_to_false_with_all_kinds_of_visisitors() {
    try {
      visitorsBridge(Arrays.asList(
        new SV1_ThrowingNPEVisitingClass(),
        new IV1_ThrowingNPEVisitingClass()),
        false)
        .visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(2);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder(
        "SV1_ThrowingNPEVisitingClass - SV1",
        "IV1_ThrowingNPEVisitingClass - IV1");
  }

  @Test
  void no_log_when_filter_execute_fine() {
    VisitorsBridge visitorsBridge = visitorsBridge(Arrays.asList(), true);
    try {
      visitorsBridge.visitFile(COMPILATION_UNIT_TREE, false);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("No exception should be raised");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  void should_not_create_symbol_table_for_generated() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    VisitorsBridge bridge = new VisitorsBridge(Collections.emptySet(), Collections.emptyList(), sonarComponents);
    bridge.setCurrentFile(new GeneratedFile(null));
    Tree tree = new JavaTree.CompilationUnitTreeImpl(null, new ArrayList<>(), new ArrayList<>(), null, null);
    bridge.visitFile(tree, false);
    verify(sonarComponents, never()).symbolizableFor(any());
  }

  @Test
  void filter_scanner_by_java_version() {
    List<String> trace = new ArrayList<>();
    class RuleForAllJavaVersion implements JavaFileScanner, EndOfAnalysisCheck {
      @Override
      public void scanFile(JavaFileScannerContext context) {
      }

      @Override
      public void endOfAnalysis() {
        trace.add(this.getClass().getSimpleName());
      }
    }
    class RuleForJava15 implements JavaFileScanner, JavaVersionAwareVisitor, EndOfAnalysisCheck {
      @Override
      public boolean isCompatibleWithJavaVersion(JavaVersion version) {
        return version.isJava15Compatible();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
      }

      @Override
      public void endOfAnalysis() {
        trace.add(this.getClass().getSimpleName());
      }
    }
    class SubscriptionVisitorForJava10 extends IssuableSubscriptionVisitor implements JavaFileScanner, JavaVersionAwareVisitor, EndOfAnalysisCheck {
      @Override
      public boolean isCompatibleWithJavaVersion(JavaVersion version) {
        return version.isJava10Compatible();
      }

      @Override
      public List<Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.TOKEN);
      }

      @Override
      public void endOfAnalysis() {
        trace.add(this.getClass().getSimpleName());
      }
    }
    List<JavaFileScanner> visitors = Arrays.asList(
      new RuleForAllJavaVersion(),
      new RuleForJava15(),
      new SubscriptionVisitorForJava10());
    VisitorsBridge visitorsBridge = new VisitorsBridge(visitors, Collections.emptyList(), null);
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion", "RuleForJava15", "SubscriptionVisitorForJava10");

    trace.clear();
    visitorsBridge.setJavaVersion(new JavaVersionImpl(8));
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion");

    trace.clear();
    visitorsBridge.setJavaVersion(new JavaVersionImpl(11));
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion", "SubscriptionVisitorForJava10");

    trace.clear();
    visitorsBridge.setJavaVersion(new JavaVersionImpl(16));
    visitorsBridge.endOfAnalysis();
    assertThat(trace).containsExactly("RuleForAllJavaVersion", "RuleForJava15", "SubscriptionVisitorForJava10");
  }

  @Test
  void canSkipScanningOfUnchangedFiles_returns_false_by_default() {
    VisitorsBridge vb = visitorsBridge(Collections.emptyList(), true);
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isFalse();
  }

  @Test
  void canSkipScanningOfUnchangedFiles_returns_based_on_context() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    VisitorsBridge vb = new VisitorsBridge(
      Collections.emptyList(),
      Collections.emptyList(),
      sonarComponents
    );

    doReturn(true).when(sonarComponents).canSkipUnchangedFiles();
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isTrue();

    doReturn(false).when(sonarComponents).canSkipUnchangedFiles();
    assertThat(vb.canSkipScanningOfUnchangedFiles()).isFalse();
  }

  @Test
  void visitorCanSkipUnchangedFiles_returns_false_for_EndOfAnalysisChecks() {
    Object visitor = new EndOfAnalysisVisitor();
    assertThat(VisitorsBridge.visitorCanSkipUnchangedFiles(visitor)).isFalse();
  }

  @Test
  void visitorCanSkipUnchangedFiles_returns_false_for_visitors_defined_outside_of_checks_package() {
    Object visitor = new VisitorOutOfChecksPackage();
    assertThat(VisitorsBridge.visitorCanSkipUnchangedFiles(visitor)).isFalse();
  }

  @Test
  void visitorCanSkipUnchangedFiles_returns_true_for_valid_visitors() {
    Object visitor = new VisitorThatCanSkip();
    assertThat(VisitorsBridge.visitorCanSkipUnchangedFiles(visitor)).isTrue();
  }

  private static String ruleKeyFromErrorLog(String errorLog) {
    String newString = errorLog.substring("Unable to run check class ".length(), errorLog.indexOf(" on file"));
    if (newString.contains("SymbolicExecutionVisitor")) {
      return "SE";
    }
    return newString.substring(newString.lastIndexOf("$") + 1);
  }

  private final VisitorsBridge visitorsBridge(JavaFileScanner visitor, boolean failOnException) {
    return visitorsBridge(Collections.singletonList(visitor), failOnException);
  }

  private final VisitorsBridge visitorsBridge(Collection<JavaFileScanner> visitors, boolean failOnException) {
    SensorContextTester sensorContextTester = SensorContextTester.create(new File(""));
    sensorContextTester.setSettings(new MapSettings().setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, failOnException));

    sonarComponents = new SonarComponents(null, null, null, null, null);
    sonarComponents.setSensorContext(sensorContextTester);

    VisitorsBridge visitorsBridge = new VisitorsBridge(visitors, new ArrayList<>(), sonarComponents);
    visitorsBridge.setCurrentFile(INPUT_FILE);

    return visitorsBridge;
  }

  @org.sonar.check.Rule(key = "JFS")
  private static class JFS_ThrowingNPEJavaFileScanner implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SV1")
  private static class SV1_ThrowingNPEVisitingClass extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SV2")
  private static class SV2_ThrowingNPELeavingClass extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void leaveNode(Tree tree) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SV3")
  private static class SV3_ThrowingNPEVisitingToken extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TOKEN);
    }

    @Override
    public void visitToken(SyntaxToken syntaxToken) {
      if ("{".equals(syntaxToken.text())) {
        // so it only throws once and not on every token
        throw NPE;
      }
    }
  }

  @org.sonar.check.Rule(key = "SV4")
  private static class SV4_ThrowingNPEVisitingTrivia extends SubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "IV1")
  private static class IV1_ThrowingNPEVisitingClass extends IssuableSubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "IV2")
  private static class IV2_ThrowingNPELeavingClass extends IssuableSubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void leaveNode(Tree tree) {
      throw NPE;
    }
  }

  private static class VisitorOutOfChecksPackage extends IssuableSubscriptionVisitor {
    @Override
    public List<Kind> nodesToVisit() {
      return null;
    }
  }
}
