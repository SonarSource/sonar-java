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
package org.sonar.java.se.xproc;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.TestUtils;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.Sema;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.CheckerDispatcher;
import org.sonar.java.se.Pair;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SETestUtils;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitorAndSemantic;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;

@EnableRuleMigrationSupport
class BehaviorCacheTest {

  @Rule
  public LogTester logTester  = new LogTester();

  @Test
  void method_behavior_cache_should_be_filled_and_cleanup() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodBehavior.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(4);
    assertThat(sev.behaviorCache.behaviors.values().stream().filter(mb -> mb != null).count()).isEqualTo(4);
    // check order of method exploration : last is the topMethod as it requires the other to get its behavior.
    // Then, as we explore fully a path before switching to another one (see the LIFO in EGW) : qix is handled before foo.
    assertThat(sev.behaviorCache.behaviors.keySet().stream().collect(Collectors.toList())).containsSequence(
      "MethodBehavior#topMethod(Z)Z",
      "MethodBehavior#bar(Z)Z",
      "MethodBehavior#foo(Z)Z",
      "MethodBehavior#independent()V");

    // method which can be overriden should not have behaviors: 'abstractMethod', 'publicMethod', 'nativeMethod'
    assertThat(sev.behaviorCache.behaviors.keySet().stream()
      .filter(s -> s.equals("#nativeMethod") || s.contains("#abstractMethod") || s.contains("#publicMethod"))
      .map(s -> sev.behaviorCache.behaviors.get(s))).isEmpty();

    assertThat(sev.behaviorCache.behaviors).hasSize(4);
    sev.behaviorCache.cleanup();
    assertThat(sev.behaviorCache.behaviors).isEmpty();
  }

  @Test
  void compute_behavior_only_once() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/ComputeBehaviorOnce.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(5);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsOnlyOnce("Could not complete symbolic execution: ");
    assertThat(sev.behaviorCache.behaviors.values()).allMatch(MethodBehavior::isVisited);
  }

  @Test
  void hardcoded_behaviors_requires_semantic() throws Exception {
    BehaviorCache behaviorCache = new BehaviorCache();
    assertThat(behaviorCache.hardcodedBehaviors()).isEmpty();
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/ComputeBehaviorOnce.java");
    assertThat(sev.behaviorCache.hardcodedBehaviors()).isNotEmpty();
  }

  @Test
  void explore_method_with_recursive_call() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/RecursiveCall.java");
    assertThat(sev.behaviorCache.behaviors).hasSize(1);
    assertThat(sev.behaviorCache.behaviors.keySet().iterator().next()).contains("#foo");
  }

  @Test
  void interrupted_exploration_does_not_create_method_yields() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/PartialMethodYieldMaxStep.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(2);

    MethodBehavior plopMethod = getMethodBehavior(sev, "foo");
    assertThat(plopMethod.isComplete()).isFalse();
    assertThat(plopMethod.yields()).isEmpty();

    MethodBehavior barMethod = getMethodBehavior(sev, "bar");
    assertThat(barMethod.isComplete()).isTrue();
    assertThat(barMethod.yields()).hasSize(2);
  }

  @Test
  void clear_stack_when_taking_exceptional_path_from_method_invocation() throws Exception {
    Pair<SymbolicExecutionVisitor, Sema> sevAndSemantic = createSymbolicExecutionVisitorAndSemantic("src/test/files/se/CleanStackWhenRaisingException.java");
    SymbolicExecutionVisitor sev = sevAndSemantic.a;
    Sema semanticModel = sevAndSemantic.b;
    MethodBehavior behavior = getMethodBehavior(sev, "foo");
    assertThat(behavior.yields()).hasSize(4);

    behavior.happyPathYields().forEach(y -> assertThat(y.resultConstraint()).isNull());
    assertThat(behavior.happyPathYields().count()).isEqualTo(1);

    List<ExceptionalYield> exceptionalYields = behavior.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(3);
    assertThat(exceptionalYields.stream().filter(y -> y.exceptionType(semanticModel).isUnknown())).hasSize(1);
  }

  @Test
  void hardcoded_behaviors() throws Exception {
    BehaviorCache behaviorCache = new BehaviorCache();
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Collections.singletonList(new NullDereferenceCheck()), behaviorCache);

    List<InputFile> inputFiles = Arrays.asList(
      "src/test/files/se/JavaLangMathMethods.java",
      "src/test/files/se/CommonsLang3StringUtilsMethods.java",
      "src/test/files/se/CommonsLang2StringUtilsMethods.java",
      "src/test/files/se/GuavaPreconditionsMethods.java",
      "src/test/files/se/ObjectsMethodsMethodBehaviors.java",
      "src/test/files/se/GuavaCommonStrings.java",
      "src/test/files/se/GuavaVerifyMethods.java",
      "src/test/files/se/CollectionUtilsIsEmpty.java",
      "src/test/files/se/CommonsLangValidate.java",
      "src/test/files/se/Log4jAssert.java",
      "src/test/files/se/SpringAssert.java",
      "src/test/files/se/EclipseAssert.java")
      .stream()
      .map(File::new)
      .map(TestUtils::inputFile)
      .collect(Collectors.toList());

    for (InputFile inputFile : inputFiles) {
      CompilationUnitTreeImpl cut = (CompilationUnitTreeImpl) JParserTestUtils.parse("test", inputFile.contents(), SETestUtils.CLASS_PATH);
      JavaFileScannerContext context = new DefaultJavaFileScannerContext(cut, inputFile, cut.sema, null, new JavaVersionImpl(8), true);
      sev.scanFile(context);
    }

    assertThat(behaviorCache.behaviors).isEmpty();
    assertThat(behaviorCache.hardcodedBehaviors()).hasSize(81);
  }

  @Test
  void java_lang_math_methods_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/JavaLangMathMethods.java");
  }

  @Test
  void commons_lang3_string_utils_method_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CommonsLang3StringUtilsMethods.java");
  }

  @Test
  void commons_lang2_string_utils_method_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CommonsLang2StringUtilsMethods.java");
  }

  @Test
  void guava_preconditions_methods_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/GuavaPreconditionsMethods.java");
  }

  @Test
  void objects_methods() {
    verifyNoIssueOnFile("src/test/files/se/ObjectsMethodsMethodBehaviors.java");
  }

  @Test
  void guava_common_Strings() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/GuavaCommonStrings.java");
  }

  @Test
  void guava_verify() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/GuavaVerifyMethods.java");
  }


  @Test
  void collections_utils_is_empty_method() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CollectionUtilsIsEmpty.java");
  }

  @Test
  void apache_lang_validate() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CommonsLangValidate.java");
  }

  @Test
  void log4j_assert() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/Log4jAssert.java");
  }

  @Test
  void spring_assert() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/SpringAssert.java");
  }

  @Test
  void eclipse_aspectj_assert() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/EclipseAssert.java");
  }

  @Test
  void test_blacklist() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/BehaviorCacheBlacklist.java");
    assertThat(sev.behaviorCache.get("java.lang.Class#getClassLoader()Ljava/lang/ClassLoader;")).isNull();
    assertThat(sev.behaviorCache.get("java.lang.Object#wait()V;")).isNull();
    assertThat(sev.behaviorCache.get("java.util.Optional#get()Ljava/lang/Object;")).isNull();
    assertThat(sev.behaviorCache.get("java.util.Optional#isPresent()Z")).isNull();
    assertThat(sev.behaviorCache.behaviors).isEmpty();
  }

  @Test
  void test_peek() throws Exception {
    Set<String> testedPre = new HashSet<>();
    Set<String> testedPost = new HashSet<>();

    SECheck check = new SECheck() {
      @Override
      public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
        if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
          Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) ((MethodInvocationTree) syntaxNode).symbol();
          MethodBehavior peekMethodBehavior = ((CheckerDispatcher) context).peekMethodBehavior(symbol);
          if ("isBlank".equals(symbol.name())) {
            assertThat(peekMethodBehavior).isNotNull();
          } else {
            assertThat(peekMethodBehavior).isNull();
          }
          testedPre.add(symbol.name());
        }
        return context.getState();
      }

      @Override
      public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
        if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
          Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) ((MethodInvocationTree) syntaxNode).symbol();
          String methodName = symbol.name();
          MethodBehavior peekMethodBehavior = ((CheckerDispatcher) context).peekMethodBehavior(symbol);
          if ("foo".equals(methodName) || "isBlank".equals(methodName)) {
            // foo should have been computed
            assertThat(peekMethodBehavior.isComplete()).isTrue();
          } else if ("bar".equals(methodName)) {
            assertThat(peekMethodBehavior).isNull();
          }
          testedPost.add(methodName);
        }
        return super.checkPostStatement(context, syntaxNode);
      }
    };
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/BehaviorCachePeek.java", check);

    assertThat(sev.behaviorCache.peek("org.apache.commons.lang.StringUtils#isBlank(Ljava/lang/String;)Z").isComplete()).isTrue();
    assertThat(sev.behaviorCache.peek("org.foo.A#foo()Z").isComplete()).isTrue();
    assertThat(sev.behaviorCache.peek("org.foo.A#bar()Z")).isNull();
    assertThat(sev.behaviorCache.peek("org.foo.A#unknownMethod()Z")).isNull();
    assertThat(sev.behaviorCache.behaviors.keySet()).containsOnly("org.foo.A#foo()Z");

    assertThat(testedPre).containsOnly("foo", "bar", "isBlank");
    assertThat(testedPost).containsOnly("foo", "bar", "isBlank");
  }

  private static void verifyNoIssueOnFile(String fileName) {
    SECheck nullDereferenceCheck = new NullDereferenceCheck();
    createSymbolicExecutionVisitorAndSemantic(fileName, nullDereferenceCheck);
    // verify we did not raise any issue, if we did, the context will get them reported.
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    nullDereferenceCheck.scanFile(context);
    verify(context, never()).reportIssueWithFlow(eq(nullDereferenceCheck), any(Tree.class), anyString(), anySet(), nullable(Integer.class));
  }

}
