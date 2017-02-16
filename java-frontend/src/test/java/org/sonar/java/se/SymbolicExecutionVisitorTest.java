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
package org.sonar.java.se;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SymbolicExecutionVisitorTest {

  private static List<File> classPath;

  @BeforeClass
  public static void setUp() throws Exception {
    File testJars = new File("target/test-jars");
    classPath = new ArrayList<>(FileUtils.listFiles(testJars, new String[]{"jar", "zip"}, true));
    classPath.add(new File("target/test-classes"));
  }

  @Test
  public void method_behavior_cache_should_be_filled() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodBehavior.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(4);
    assertThat(sev.behaviorCache.behaviors.values().stream().filter(mb -> mb != null).count()).isEqualTo(4);
    // check order of method exploration : last is the topMethod as it requires the other to get its behavior.
    // Then, as we explore fully a path before switching to another one (see the LIFO in EGW) : qix is handled before foo.
    assertThat(sev.behaviorCache.behaviors.keySet().stream().map(Symbol.MethodSymbol::name).collect(Collectors.toList()))
      .containsSequence("topMethod", "bar", "foo", "independent");

    // method which can be overriden should not have behaviors: 'abstractMethod', 'publicMethod', 'nativeMethod'
    assertThat(sev.behaviorCache.behaviors.keySet().stream()
      .filter(s -> "nativeMethod".equals(s.name()) || "abstractMethod".equals(s.name()) || "publicMethod".equals(s.name()))
      .map(s -> sev.behaviorCache.behaviors.get(s))).isEmpty();
  }

  @Test
  public void method_behavior_yields() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodYields.java");

    MethodBehavior mb = getMethodBehavior(sev, "method");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(3);

    List<MethodYield> trueResults = yields.stream().filter(my -> BooleanConstraint.TRUE.equals(my.resultConstraint)).collect(Collectors.toList());
    assertThat(trueResults).hasSize(1);
    MethodYield trueResult = trueResults.get(0);

    // 'a' has constraint "null"
    assertThat(trueResult.parametersConstraints[0].isNull()).isTrue();
    // no constraint on 'b'
    assertThat(trueResult.parametersConstraints[1]).isNull();
    // result SV is a different SV than 'a' and 'b'
    assertThat(trueResult.resultIndex).isEqualTo(-1);

    List<MethodYield> falseResults = yields.stream().filter(my -> BooleanConstraint.FALSE.equals(my.resultConstraint)).collect(Collectors.toList());
    assertThat(falseResults).hasSize(2);
    // for both "False" results, 'a' has the constraint "not null"
    assertThat(falseResults.stream().filter(my -> !my.parametersConstraints[0].isNull()).count()).isEqualTo(2);
    // 1) 'b' has constraint "false", result is 'b'
    assertThat(falseResults.stream().filter(my -> BooleanConstraint.FALSE.equals(my.parametersConstraints[1]) && my.resultIndex == 1).count()).isEqualTo(1);

    // 2) 'b' is "true", result is a different SV than 'a' and 'b'
    assertThat(falseResults.stream().filter(my -> BooleanConstraint.TRUE.equals(my.parametersConstraints[1]) && my.resultIndex == -1).count()).isEqualTo(1);
  }

  @Test
  public void method_behavior_handling_finally() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/ReturnAndFinally.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(2);

    MethodBehavior foo = getMethodBehavior(sev, "foo");
    assertThat(foo.yields()).hasSize(4);
    assertThat(foo.yields().stream().filter(y -> !y.exception).count()).isEqualTo(2);
    assertThat(foo.yields().stream().filter(y -> y.exception).count()).isEqualTo(2);

    MethodBehavior qix = getMethodBehavior(sev, "qix");
    List<MethodYield> qixYield = qix.yields();
    assertThat(qixYield.stream()
      .filter(y -> !y.parametersConstraints[0].isNull())
      .allMatch(y -> y.exception)).isTrue();

    assertThat(qixYield.stream()
      .filter(y -> y.parametersConstraints[0].isNull() && y.exception)
      .count()).isEqualTo(2);

    assertThat(qixYield.stream()
      .filter(y -> !y.exception)
      .allMatch(y -> y.parametersConstraints[0].isNull())).isTrue();
  }

  @Test
  public void explore_method_with_recursive_call() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/RecursiveCall.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(1);
    assertThat(sev.behaviorCache.behaviors.keySet().iterator().next().name()).isEqualTo("foo");
  }

  @Test
  public void interrupted_exploration_does_not_create_method_yields() throws Exception {
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
  public void clear_stack_when_taking_exceptional_path_from_method_invocation() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/CleanStackWhenRaisingException.java");
    List<MethodYield> yields = sev.behaviorCache.behaviors.values().iterator().next().yields();
    assertThat(yields).hasSize(5);
    yields.stream().map(y -> y.resultConstraint).filter(Objects::nonNull).forEach(c -> assertThat(c.isNull()).isFalse());
    assertThat(yields.stream().filter(y -> !y.exception).count()).isEqualTo(2);
    List<MethodYield> exceptionalYields = yields.stream().filter(y -> y.exception).collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(3);
    assertThat(exceptionalYields.stream().filter(y -> y.exceptionType == null)).hasSize(1);
    // exception thrown by System.getProperty()
    assertThat(exceptionalYields.stream()
      .filter(y -> y.exceptionType != null)
      .map(y -> y.exceptionType.fullyQualifiedName()))
        .containsOnly("java.lang.SecurityException", "java.lang.IllegalArgumentException");
  }

  @Test
  public void commons_lang3_string_utils_method_should_be_handled() throws Exception {
    NullDereferenceCheck seCheck = new NullDereferenceCheck();
    createSymbolicExecutionVisitor("src/test/files/se/CommonsLang3StringUtilsMethods.java", seCheck);
    // verify we did not raise any issue, if we did, the context will get them reported.
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    seCheck.scanFile(context);
    verify(context, never()).reportIssueWithFlow(eq(seCheck), any(Tree.class), anyString(), anySet(), anyInt());
  }

  @Test
  public void commons_lang2_string_utils_method_should_be_handled() throws Exception {
    NullDereferenceCheck seCheck = new NullDereferenceCheck();
    createSymbolicExecutionVisitor("src/test/files/se/CommonsLang2StringUtilsMethods.java", seCheck);
    // verify we did not raise any issue, if we did, the context will get them reported.
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    seCheck.scanFile(context);
    verify(context, never()).reportIssueWithFlow(eq(seCheck), any(Tree.class), anyString(), anySet(), anyInt());
  }

  @Test
  public void guava_preconditions_methods_should_be_handled() throws Exception {
    NullDereferenceCheck seCheck = new NullDereferenceCheck();
    createSymbolicExecutionVisitor("src/test/files/se/GuavaPreconditionsMethods.java", seCheck);
    // verify we did not raise any issue, if we did, the context will get them reported.
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    seCheck.scanFile(context);
    verify(context, never()).reportIssueWithFlow(eq(seCheck), any(Tree.class), anyString(), anySet(), anyInt());
  }

  private static SymbolicExecutionVisitor createSymbolicExecutionVisitor(String fileName) {
    return createSymbolicExecutionVisitor(fileName, new NullDereferenceCheck());
  }

  private static SymbolicExecutionVisitor createSymbolicExecutionVisitor(String fileName, SECheck seCheck) {
    ActionParser<Tree> p = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) p.parse(new File(fileName));
    SemanticModel semanticModel = SemanticModel.createFor(cut, classPath);
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Lists.newArrayList(seCheck));
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getTree()).thenReturn(cut);
    when(context.getSemanticModel()).thenReturn(semanticModel);
    sev.scanFile(context);
    return sev;
  }

  private static MethodBehavior getMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    Optional<MethodBehavior> mb = sev.behaviorCache.behaviors.entrySet().stream()
      .filter(e -> methodName.equals(e.getKey().name()))
      .map(Map.Entry::getValue)
      .findFirst();
    assertThat(mb.isPresent()).isTrue();
    return mb.get();
  }
}