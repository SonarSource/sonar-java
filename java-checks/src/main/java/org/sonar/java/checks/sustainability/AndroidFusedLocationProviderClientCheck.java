/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.sustainability;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6914")
public class AndroidFusedLocationProviderClientCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofSubTypes("android.content.Context")
      .names("getSystemService")
      .addParametersMatcher("java.lang.String")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    var nameArg = tree.arguments().get(0);
    if ("location".equals(ExpressionUtils.resolveAsConstant(nameArg))) {
      reportIfInAndroidContext(nameArg);
    }
  }

  private void reportIfInAndroidContext(Tree tree) {
    if (context.inAndroidContext()) {
      reportIssue(tree, "Use \"FusedLocationProviderClient\" instead of \"LocationManager\".");
    }
  }
}
