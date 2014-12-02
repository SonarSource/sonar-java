/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.java.JavaAstScanner;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
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
    class Visitor extends AbstractMethodDetection {

      public List<Integer> lines = Lists.newArrayList();

      @Override
      protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
        return ImmutableList.of(MethodInvocationMatcher.create().typeDefinition("A").name("method").addParameter("int"));
      }

      @Override
      protected void onMethodFound(MethodInvocationTree tree) {
        lines.add(((JavaTree) tree).getLine());
      }

    }
    Visitor visitor = new Visitor();
    JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AbstractMethodDetection.java"), new VisitorsBridge(visitor));

    assertThat(visitor.lines).hasSize(1);
    assertThat(visitor.lines).containsExactly(10);
  }

}