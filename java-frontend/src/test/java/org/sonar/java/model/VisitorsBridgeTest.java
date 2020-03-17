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
package org.sonar.java.model;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.Fail;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.AnalysisException;
import org.sonar.java.CheckFailureException;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class VisitorsBridgeTest {

  @Rule
  public LogTester logTester = new LogTester();

  private SonarComponents sonarComponents = null;

  private static final File FILE = new File("src/test/files/model/SimpleClass.java");
  private static final InputFile INPUT_FILE = TestUtils.inputFile(FILE);
  private static final CompilationUnitTree COMPILATION_UNIT_TREE = JParserTestUtils.parse(FILE);

  private static final NullPointerException NPE = new NullPointerException("BimBadaboum");

  @Test
  @Ignore
  public void test_semantic_exclusions() {
    VisitorsBridge visitorsBridgeWithoutSemantic = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNull();
      assertThat(context.fileParsed()).isTrue();
    }), new ArrayList<>(), null);
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
        return Collections.singletonList(Tree.Kind.METHOD);
      }
    }), new ArrayList<>(), null);
    checkFile(contstructFileName("org", "foo", "bar", "Foo.java"), "class Foo { arrrrrrgh", visitorsBridgeWithParsingIssue);
  }

  private static void checkFile(String filename, String code, VisitorsBridge visitorsBridge) {
    visitorsBridge.setCurrentFile(TestUtils.emptyInputFile(filename));
    visitorsBridge.visitFile(parse(code));
  }

  @Test
  @Ignore
  public void log_only_50_elements() {
    DecimalFormat formatter = new DecimalFormat("00");
    IntFunction<String> classNotFoundName = i -> "NotFound" + formatter.format(i);
    VisitorsBridge visitorsBridge = new VisitorsBridge(Collections.singletonList((JavaFileScanner) context -> {
      assertThat(context.getSemanticModel()).isNotNull();
      // FIXME log missing classes?
      // ((SemanticModel) context.getSemanticModel()).classesNotFound().addAll(IntStream.range(0,
      // 60).mapToObj(classNotFoundName).collect(Collectors.toList()));
    }), new ArrayList<>(), null);
    checkFile("Foo.java", "class Foo {}", visitorsBridge);
    visitorsBridge.endOfAnalysis();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Classes not found during the analysis : [" +
        IntStream.range(0, 50 /* only first 50 missing classes are displayed in the log */).mapToObj(classNotFoundName).sorted().collect(Collectors.joining(", ")) + ", ...]");
  }

  private static String contstructFileName(String... path) {
    String result = "";
    for (String s : path) {
      result += s + File.separator;
    }
    return result.substring(0, result.length() - 1);
  }

  private static CompilationUnitTree parse(String code) {
    return JParserTestUtils.parse(code);
  }

  @Test
  public void rethrow_exception_when_hidden_property_set_to_true_with_JavaFileScanner() {
    try {
      visitorsBridge(new JFS_ThrowingNPEJavaFileScanner(), true)
        .visitFile(COMPILATION_UNIT_TREE);
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
  public void swallow_exception_when_hidden_property_set_to_false_with_JavaFileScanner() {
    try {
      visitorsBridge(new JFS_ThrowingNPEJavaFileScanner(), false)
        .visitFile(COMPILATION_UNIT_TREE);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("JFS_ThrowingNPEJavaFileScanner - JFS");
  }

  @Test
  public void rethrow_exception_when_hidden_property_set_to_true_with_SubscriptionVisitor() {
    try {
      visitorsBridge(new SV1_ThrowingNPEVisitingClass(), true)
        .visitFile(COMPILATION_UNIT_TREE);
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
  public void swallow_exception_when_hidden_property_set_to_false_with_SubscriptionVisitor() {
    try {
      visitorsBridge(Arrays.asList(
        new SV1_ThrowingNPEVisitingClass(),
        new SV2_ThrowingNPELeavingClass(),
        new SV3_ThrowingNPEVisitingToken(),
        new SV4_ThrowingNPEVisitingTrivia()),
        false)
        .visitFile(COMPILATION_UNIT_TREE);
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
  public void swallow_exception_when_hidden_property_set_to_false_with_IssuableSubscriptionVisitor() {
    try {
      visitorsBridge(Arrays.asList(
        new IV1_ThrowingNPEVisitingClass(),
        new IV2_ThrowingNPELeavingClass()),
        false)
        .visitFile(COMPILATION_UNIT_TREE);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsOnly("IV1_ThrowingNPEVisitingClass - IV1");
  }

  @Test
  public void swallow_exception_when_hidden_property_set_to_false_with_all_kinds_of_visisitors() {
    try {
      visitorsBridge(Arrays.asList(
        new SE1_ThrowingNPEPreStatement(),
        new SV1_ThrowingNPEVisitingClass(),
        new IV1_ThrowingNPEVisitingClass()),
        false)
        .visitFile(COMPILATION_UNIT_TREE);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exceptions should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(3);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder(
        "SE",
        "SV1_ThrowingNPEVisitingClass - SV1",
        "IV1_ThrowingNPEVisitingClass - IV1");
  }

  @Test
  public void rethrow_exception_when_hidden_property_set_to_true_with_SECheck() {
    try {
      visitorsBridge(Arrays.asList(
        new SE1_ThrowingNPEPreStatement(),
        new SE2_ThrowingNPEPostStatement()), true)
        .visitFile(COMPILATION_UNIT_TREE);
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
      .containsExactlyInAnyOrder("SE");
  }

  @Test
  public void swallow_exception_when_hidden_property_set_to_false_with_SECheck() {
    try {
      visitorsBridge(Arrays.asList(
        new SE1_ThrowingNPEPreStatement(),
        new SE2_ThrowingNPEPostStatement()), false)
        .visitFile(COMPILATION_UNIT_TREE);
    } catch (Exception e) {
      e.printStackTrace();
      Fail.fail("Exception should be swallowed when property is not set");
    }
    assertThat(logTester.logs(LoggerLevel.ERROR)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.ERROR).stream().map(VisitorsBridgeTest::ruleKeyFromErrorLog))
      .containsExactlyInAnyOrder("SE");
  }

  @Test
  public void should_not_create_symbol_table_for_generated() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    VisitorsBridge bridge = new VisitorsBridge(Collections.emptySet(), Collections.emptyList(), sonarComponents);
    bridge.setCurrentFile(new GeneratedFile(null));
    Tree tree = new JavaTree.CompilationUnitTreeImpl(null, new ArrayList<>(), new ArrayList<>(), null, null);
    bridge.visitFile(tree);
    verify(sonarComponents, never()).symbolizableFor(any());
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

    VisitorsBridge visitorsBridge = new VisitorsBridge(visitors, new ArrayList<>(), sonarComponents, SymbolicExecutionMode.ENABLED_WITHOUT_X_FILE);
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

  @org.sonar.check.Rule(key = "SE1")
  private static class SE1_ThrowingNPEPreStatement extends SECheck {
    @Override
    public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
      throw NPE;
    }
  }

  @org.sonar.check.Rule(key = "SE2")
  private static class SE2_ThrowingNPEPostStatement extends SECheck {
    @Override
    public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
      throw NPE;
    }
  }

}
