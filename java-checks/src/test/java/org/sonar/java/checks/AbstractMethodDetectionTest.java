/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class AbstractMethodDetectionTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {

    Visitor visitor = new Visitor(ImmutableList.of(
      MethodMatcher.create().typeDefinition("A").name("method").addParameter("int"),
      MethodMatcher.create().typeDefinition("A").name("method").addParameter("java.lang.String[]")
      ));
    JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AbstractMethodDetection.java"), new VisitorsBridge(visitor));

    assertThat(visitor.lines).hasSize(2);
    assertThat(visitor.lines).containsExactly(13, 15);
  }

  @Test
  public void withNoParameterConstraint() throws Exception {
    Visitor visitor = new Visitor(ImmutableList.of(
      MethodMatcher.create().typeDefinition("A").name("method").withNoParameterConstraint()
      ));
    JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AbstractMethodDetection.java"), new VisitorsBridge(visitor));

    assertThat(visitor.lines).containsExactly(13, 14, 15);

  }

  class Visitor extends AbstractMethodDetection {

    public List<Integer> lines = Lists.newArrayList();
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

  }

}