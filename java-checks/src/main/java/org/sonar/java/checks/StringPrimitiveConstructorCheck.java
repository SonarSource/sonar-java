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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2129")
public class StringPrimitiveConstructorCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final String INIT = "<init>";
  private static final BigInteger MIN_BIG_INTEGER_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger MAX_BIG_INTEGER_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(STRING).name(INIT).withoutParameter(),
      MethodMatcher.create().typeDefinition(STRING).name(INIT).addParameter(STRING),
      MethodMatcher.create().typeDefinition("java.lang.Byte").name(INIT).addParameter("byte"),
      MethodMatcher.create().typeDefinition("java.lang.Character").name(INIT).addParameter("char"),
      MethodMatcher.create().typeDefinition("java.lang.Short").name(INIT).addParameter("short"),
      MethodMatcher.create().typeDefinition("java.lang.Integer").name(INIT).addParameter("int"),
      MethodMatcher.create().typeDefinition("java.lang.Long").name(INIT).addParameter("long"),
      MethodMatcher.create().typeDefinition("java.lang.Float").name(INIT).addParameter("float"),
      MethodMatcher.create().typeDefinition("java.lang.Double").name(INIT).addParameter("double"),
      MethodMatcher.create().typeDefinition("java.lang.Boolean").name(INIT).addParameter("boolean"),
      MethodMatcher.create().typeDefinition("java.math.BigInteger").name(INIT).addParameter(STRING)
    );
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if(isBigIntegerPotentiallyBiggerThanLong(newClassTree)) {
      return;
    }
    reportIssue(newClassTree.identifier(), "Remove this \"" + newClassTree.symbolType().name() + "\" constructor");
  }

  private static boolean isBigIntegerPotentiallyBiggerThanLong(NewClassTree newClassTree) {
    if (!newClassTree.symbolType().is("java.math.BigInteger")) {
      return false;
    }
    ExpressionTree argument = newClassTree.arguments().get(0);
    if (!argument.is(Tree.Kind.STRING_LITERAL)) {
      return true;
    }
    try {
      BigInteger value = new BigInteger(LiteralUtils.trimQuotes(((LiteralTree)argument).value()));
      return value.compareTo(MIN_BIG_INTEGER_VALUE) < 0 || value.compareTo(MAX_BIG_INTEGER_VALUE) > 0;
    } catch (NumberFormatException e) {
      return true;
    }
  }
}
