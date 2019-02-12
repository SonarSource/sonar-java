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

import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S3658")
public class AssertionFailInCatchBlockCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Lists.newArrayList(
      MethodMatcher.create().typeDefinition("org.junit.Assert").name("fail").withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.junit.jupiter.api.Assertions").name("fail").withAnyParameters(),
      MethodMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("fail")).withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.fest.assertions.Fail").name(NameCriteria.startsWith("fail")).withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (isInCatch(mit)) {
      reportIssue(mit, "Remove this failure assertion and simply add the exception type to the method signature.");
    }
  }

  private static boolean isInCatch(Tree tree) {
    Tree parent = tree.parent();
    return parent != null && !parent.is(Kind.METHOD) && (parent.is(Kind.CATCH) || isInCatch(parent));
  }
}
