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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1449")
public class StringMethodsWithLocaleCheck extends AbstractMethodDetection {


  private static final String STRING = "java.lang.String";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition(STRING).name("toUpperCase").withoutParameter(),
      MethodMatcher.create().typeDefinition(STRING).name("toLowerCase").withoutParameter()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree report = mit.methodSelect();
    if(report.is(Tree.Kind.MEMBER_SELECT)) {
      report = ((MemberSelectExpressionTree) report).identifier();
    }
    reportIssue(report, "Define the locale to be used in this String operation.");
  }
}
