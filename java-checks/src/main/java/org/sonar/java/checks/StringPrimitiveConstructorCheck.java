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
import org.sonar.plugins.java.api.tree.NewClassTree;

import java.util.List;

@Rule(key = "S2129")
public class StringPrimitiveConstructorCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final String INIT = "<init>";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition(STRING).name(INIT).withoutParameter(),
      MethodMatcher.create().typeDefinition(STRING).name(INIT).addParameter(STRING),
      MethodMatcher.create().typeDefinition("java.lang.Byte").name(INIT).addParameter("byte"),
      MethodMatcher.create().typeDefinition("java.lang.Character").name(INIT).addParameter("char"),
      MethodMatcher.create().typeDefinition("java.lang.Short").name(INIT).addParameter("short"),
      MethodMatcher.create().typeDefinition("java.lang.Integer").name(INIT).addParameter("int"),
      MethodMatcher.create().typeDefinition("java.lang.Long").name(INIT).addParameter("long"),
      MethodMatcher.create().typeDefinition("java.lang.Float").name(INIT).addParameter("float"),
      MethodMatcher.create().typeDefinition("java.lang.Double").name(INIT).addParameter("double"),
      MethodMatcher.create().typeDefinition("java.lang.Boolean").name(INIT).addParameter("boolean")
    );
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree.identifier(), "Remove this \""+newClassTree.symbolType().name()+"\" constructor");
  }
}
