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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;


public class BehaviorCacheTest {

  @Rule
  public LogTester logTester  = new LogTester();

  @Test
  public void method_behavior_cache_should_be_filled() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodBehavior.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(5);
    assertThat(sev.behaviorCache.behaviors.values().stream().filter(mb -> mb != null).count()).isEqualTo(5);
    // check order of method exploration : last is the topMethod as it requires the other to get its behavior.
    // Then, as we explore fully a path before switching to another one (see the LIFO in EGW) : qix is handled before foo.
    assertThat(sev.behaviorCache.behaviors.keySet().stream().collect(Collectors.toList())).containsSequence(
      "MethodBehavior#topMethod(Z)Z",
      "MethodBehavior#bar(Z)Z",
      // String is final, so length() can not be overridden
      "java.lang.String#length()I",
      "MethodBehavior#foo(Z)Z",
      "MethodBehavior#independent()V");

    // method which can be overriden should not have behaviors: 'abstractMethod', 'publicMethod', 'nativeMethod'
    assertThat(sev.behaviorCache.behaviors.keySet().stream()
      .filter(s -> s.equals("#nativeMethod") || s.contains("#abstractMethod") || s.contains("#publicMethod"))
      .map(s -> sev.behaviorCache.behaviors.get(s))).isEmpty();
  }

  @Test
  public void compute_beahvior_only_once() throws Exception {
    SymbolicExecutionVisitor sev = spy(createSymbolicExecutionVisitor("src/test/resources/se/ComputeBehaviorOnce.java"));
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(5);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).containsOnlyOnce("Could not complete symbolic execution: ");
    assertThat(sev.behaviorCache.behaviors.values()).allMatch(MethodBehavior::isVisited);
  }

  @Test
  public void explore_method_with_recursive_call() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/RecursiveCall.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(1);
    assertThat(sev.behaviorCache.behaviors.keySet().iterator().next()).contains("#foo");
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
    MethodBehavior behavior = getMethodBehavior(sev, "foo");
    assertThat(behavior.yields()).hasSize(3);

    behavior.happyPathYields().map(y -> y.resultConstraint()).filter(Objects::nonNull).forEach(pMap -> assertThat(pMap.get(ObjectConstraint.class) == ObjectConstraint.NULL).isFalse());
    assertThat(behavior.happyPathYields().count()).isEqualTo(2);

    List<ExceptionalYield> exceptionalYields = behavior.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(1);
    assertThat(exceptionalYields.stream().filter(y -> y.exceptionType() == null)).hasSize(1);
  }

  @Test
  public void commons_lang3_string_utils_method_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CommonsLang3StringUtilsMethods.java");
  }

  @Test
  public void commons_lang2_string_utils_method_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CommonsLang2StringUtilsMethods.java");
  }

  @Test
  public void guava_preconditions_methods_should_be_handled() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/GuavaPreconditionsMethods.java");
  }

  @Test
  public void collections_utils_is_empty_method() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CollectionUtilsIsEmpty.java");
  }

  @Test
  public void apache_lang_validate() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/CommonsLangValidate.java");
  }

  @Test
  public void log4j_and_spring_assert() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/SpringAndLog4jAssert.java");
  }

  @Test
  public void eclipse_aspectj_assert() throws Exception {
    verifyNoIssueOnFile("src/test/files/se/EclipseAssert.java");
  }

  private static void verifyNoIssueOnFile(String fileName) {
    NullDereferenceCheck seCheck = new NullDereferenceCheck();
    createSymbolicExecutionVisitor(fileName, seCheck);
    // verify we did not raise any issue, if we did, the context will get them reported.
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    seCheck.scanFile(context);
    verify(context, never()).reportIssueWithFlow(eq(seCheck), any(Tree.class), anyString(), anySet(), nullable(Integer.class));
  }

}
