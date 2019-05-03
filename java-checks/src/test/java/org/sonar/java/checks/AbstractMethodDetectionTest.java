/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.java.CheckTestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractMethodDetectionTest {

  @Test
  public void detected() {
    Visitor visitor = new Visitor(ImmutableList.of(
      MethodMatcher.create().typeDefinition("A").name("method").addParameter("int"),
      MethodMatcher.create().typeDefinition("A").name("method").addParameter("java.lang.String[]")
      ));
    JavaAstScanner.scanSingleFileForTests(CheckTestUtils.inputFile("src/test/files/checks/AbstractMethodDetection.java"), new VisitorsBridge(visitor));

    assertThat(visitor.lines).hasSize(3);
    assertThat(visitor.lines).containsExactly(15, 17, 19);
  }

  @Test
  public void withAnyParameters() throws Exception {
    Visitor visitor = new Visitor(Collections.singletonList(
      MethodMatcher.create().typeDefinition("A").name("method").withAnyParameters()
      ));
    JavaAstScanner.scanSingleFileForTests(CheckTestUtils.inputFile("src/test/files/checks/AbstractMethodDetection.java"), new VisitorsBridge(visitor));

    assertThat(visitor.lines).containsExactly(14, 15, 16, 17, 19);
  }

  @Test
  public void withoutParameter() throws Exception {
    Visitor visitor = new Visitor(Collections.singletonList(
      MethodMatcher.create().typeDefinition("A").name("method").withoutParameter()
      ));
    JavaAstScanner.scanSingleFileForTests(CheckTestUtils.inputFile("src/test/files/checks/AbstractMethodDetection.java"), new VisitorsBridge(visitor));

    assertThat(visitor.lines).containsExactly(14);
  }

  class Visitor extends AbstractMethodDetection {

    public List<Integer> lines = new ArrayList<>();
    private List<MethodMatcher> methodInvocationMatchers;

    public Visitor(List<MethodMatcher> methodInvocationMatchers) {
      this.methodInvocationMatchers = methodInvocationMatchers;
    }

    @Override
    protected List<MethodMatcher> getMethodInvocationMatchers() {
      return methodInvocationMatchers;
    }

    @Override
    protected void onMethodInvocationFound(MethodInvocationTree tree) {
      lines.add(((JavaTree) tree).getLine());
    }

    @Override
    protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
      lines.add(((JavaTree) methodReferenceTree).getLine());
    }
  }

}
