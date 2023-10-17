/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6806")
public class ModelAttributeNamingConventionForSpELCheck extends AbstractMethodDetection {

  private static final MethodMatchers ADD_ATTRIBUTE_MATCHER_WITH_TWO_PARAMS = MethodMatchers.create()
    .ofTypes("org.springframework.ui.Model")
    .names("addAttribute")
    .addParametersMatcher("java.lang.String", "java.lang.Object")
    .build();

  private static final MethodMatchers ADD_ATTRIBUTE_MATCHER_WITH_ONE_PARAM = MethodMatchers.create()
    .ofTypes("org.springframework.ui.Model")
    .names("addAllAttributes")
    .addParametersMatcher("java.util.Map")
    .build();

  private static final MethodMatchers MAP_OF = MethodMatchers.create()
    .ofTypes("java.util.Map")
    .names("of")
    .withAnyParameters()
    .build();

  Pattern pattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(ADD_ATTRIBUTE_MATCHER_WITH_TWO_PARAMS, ADD_ATTRIBUTE_MATCHER_WITH_ONE_PARAM);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argumentTree = mit.arguments().get(0);
    checkStringLiteralAndReport(argumentTree);

    if (argumentTree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) argumentTree;
      if (MAP_OF.matches(methodInvocationTree)) {
        for (int i = 0; i < methodInvocationTree.arguments().size(); i += 2) {
          ExpressionTree tree = methodInvocationTree.arguments().get(i);
          if (checkStringLiteralAndReport(tree)) {
            break;
          }
        }
      }
    }
  }

  private boolean checkStringLiteralAndReport(ExpressionTree argumentTree) {
    if (argumentTree.is(Tree.Kind.STRING_LITERAL)) {
      LiteralTree literalTree = (LiteralTree) argumentTree;
      String literalValue = LiteralUtils.getAsStringValue(literalTree);
      Matcher matcher = pattern.matcher(literalValue);
      if (!matcher.matches()) {
        reportIssue(argumentTree,
          "Attribute names must begin with a letter (a-z, A-Z), underscore (_), or dollar sign ($) and can be followed by letters, digits, underscores, or dollar signs.");
        return true;
      }
    }
    return false;
  }

}
