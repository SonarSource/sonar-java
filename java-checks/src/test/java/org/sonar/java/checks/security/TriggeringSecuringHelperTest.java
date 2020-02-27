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
package org.sonar.java.checks.security;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.sonar.java.checks.helpers.JParserTestUtils;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TriggeringSecuringHelperTest {

  private final static MethodMatcher triggeringMethod = new MethodMatcher().typeDefinition("A").name("newInstance").withAnyParameters();

  private TriggeringSecuringHelper simpleTriggeringSecuring = new TriggeringSecuringHelper(triggeringMethod) {

    private final MethodMatcher securingMethod = new MethodMatcher().typeDefinition("A").name("securing").withAnyParameters();
    private boolean isSecured = false;

    @Override
    public void resetState() {
      isSecured = false;
    }

    @Override
    public void processSecuringMethodInvocation(MethodInvocationTree mit) {
      if (securingMethod.matches(mit)) {
        isSecured = true;
      }
    }

    @Override
    public boolean isSecured() {
      return isSecured;
    }
  };

  @Test
  public void test() {
    CompilationUnitTree cut = JParserTestUtils.parse(new File("src/test/resources/checks/security/TriggeringSecuringHelperTest.java"));
    List<Tree> members = ((ClassTreeImpl) cut.types().get(1)).members();

    // Field doesn't have enclosing method
    assertFalse(containsUnsecuredTrigger(members.get(0)));

    assertFalse(containsUnsecuredTrigger(members.get(1)));
    assertFalse(containsUnsecuredTrigger(members.get(2)));
    assertTrue(containsUnsecuredTrigger(members.get(3)));
    assertTrue(containsUnsecuredTrigger(members.get(4)));
    assertFalse(containsUnsecuredTrigger(members.get(5)));
    assertTrue(containsUnsecuredTrigger(members.get(6)));
  }

  private boolean containsUnsecuredTrigger(Tree method) {
    FindMethodInvocation methodInvocation = new FindMethodInvocation();
    method.accept(methodInvocation);
    for (MethodInvocationTree mit: methodInvocation.methodInvocationTrees) {
      if (simpleTriggeringSecuring.test(mit)) {
        return true;
      }
    }
    return false;
  }

  private static class FindMethodInvocation extends BaseTreeVisitor {
    private List<MethodInvocationTree> methodInvocationTrees = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      methodInvocationTrees.add(tree);
      super.visitMethodInvocation(tree);
    }
  }





}
