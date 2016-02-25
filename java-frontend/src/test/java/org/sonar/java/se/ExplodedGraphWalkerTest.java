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
package org.sonar.java.se;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.se.checks.ConditionAlwaysTrueOrFalseCheck;
import org.sonar.java.se.checks.CustomUnclosedResourcesCheck;
import org.sonar.java.se.checks.LocksNotUnlockedCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ExplodedGraphWalkerTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/se/SeEngineTest.java", new IssueVisitor());
  }

  @Test
  public void test_cleanup_state() {
    final int[] steps = new int[2];
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCleanupState.java", new SymbolicExecutionVisitor(Collections.<JavaFileScanner>emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker(context, false);
        tree.accept(explodedGraphWalker);
        steps[0] += explodedGraphWalker.steps;
      }
    });
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCleanupState.java", new SymbolicExecutionVisitor(Collections.<JavaFileScanner>emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        ExplodedGraphWalker explodedGraphWalker = new ExplodedGraphWalker(context);
        tree.accept(explodedGraphWalker);
        steps[1] += explodedGraphWalker.steps;
      }
    });
    assertThat(steps[0]).isPositive();
    assertThat(steps[0]).isGreaterThan(steps[1]);
  }

  @Test
  public void reproducer() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/Reproducer.java", new IssueVisitor());
  }

  @Test
  public void test_limited_loop_execution() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/SeEngineTestCase.java", new SymbolicExecutionVisitor(Collections.<JavaFileScanner>emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        try {
          tree.accept(new ExplodedGraphWalker(context));
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          fail("loop execution should be limited");
        }
      }
    });
  }

  @Test
  public void test_maximum_steps_reached() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxSteps.java", new SymbolicExecutionVisitor(Collections.<JavaFileScanner>emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        try {
          tree.accept(new ExplodedGraphWalker(context));
          fail("Too many states were processed !");
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          assertThat(exception.getMessage()).startsWith("reached limit of 10000 steps for method");
        }
      }
    });
  }

  @Test
  public void test_maximum_number_nested_states() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/MaxNestedStates.java", new SymbolicExecutionVisitor(Collections.<JavaFileScanner>emptyList()) {
      @Override
      public void visitNode(Tree tree) {
        try {
          tree.accept(new ExplodedGraphWalker(context));
          fail("Too many states were processed !");
        } catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
          assertThat(exception.getMessage()).startsWith("reached maximum number of 10000 branched states");
        }
      }
    });
  }

  class IssueVisitor implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      reportIssuesFor(context, new NullDereferenceCheck());
      reportIssuesFor(context, new ConditionAlwaysTrueOrFalseCheck());
      reportIssuesFor(context, new UnclosedResourcesCheck());
      reportIssuesFor(context, new CustomUnclosedResourcesCheck());
      reportIssuesFor(context, new LocksNotUnlockedCheck());
    }

    private void reportIssuesFor(JavaFileScannerContext context, JavaCheck check) {
      Multimap<Tree, DefaultJavaFileScannerContext.SEIssue> issues = ((DefaultJavaFileScannerContext) context).getSEIssues((Class<? extends SECheck>) check.getClass());
      for (Map.Entry<Tree, DefaultJavaFileScannerContext.SEIssue> issue : issues.entries()) {
        DefaultJavaFileScannerContext.SEIssue seIssue = issue.getValue();
        context.reportIssue(this, seIssue.getTree(), seIssue.getMessage(), seIssue.getSecondary(), null);
      }
    }
  }

}
