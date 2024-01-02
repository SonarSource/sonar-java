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
package org.sonar.java.checks.spring;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6806")
public class ModelAttributeNamingConventionForSpELCheck extends AbstractMethodDetection {

  private static final Pattern pattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

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
    .names("of", "ofEntries", "entry")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(ADD_ATTRIBUTE_MATCHER_WITH_TWO_PARAMS, ADD_ATTRIBUTE_MATCHER_WITH_ONE_PARAM);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argumentTree = mit.arguments().get(0);
    checkExpression(argumentTree, argumentTree);
  }

  private void checkExpression(ExpressionTree argumentTree, ExpressionTree reportTree) {
    if (argumentTree.is(Tree.Kind.STRING_LITERAL)) {
      checkStringLiteralAndReport(argumentTree, reportTree);
    } else if (argumentTree.is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) argumentTree);
    } else if (argumentTree.is(Tree.Kind.MEMBER_SELECT)) {
      checkMemberSelect((MemberSelectExpressionTree) argumentTree);
    } else if (argumentTree.is(Tree.Kind.METHOD_INVOCATION)) {
      checkMethodInvocation((MethodInvocationTree) argumentTree);
    }
  }

  private void checkStringLiteralAndReport(ExpressionTree tree, ExpressionTree reportTree) {
    LiteralTree literalTree = (LiteralTree) tree;
    String literalValue = LiteralUtils.getAsStringValue(literalTree);
    Matcher matcher = pattern.matcher(literalValue);
    if (!matcher.matches()) {
      reportIssue(reportTree,
        "Attribute names must begin with a letter (a-z, A-Z), underscore (_), or dollar sign ($) and can be followed by letters, digits, underscores, or dollar signs.");
    }
  }

  private void checkIdentifier(IdentifierTree identifierTree) {
    VariableTreeImpl declaration = (VariableTreeImpl) identifierTree.symbol().declaration();
    if (declaration != null && declaration.initializer()!=null) {
      checkExpression(declaration.initializer(), identifierTree);
    }
  }

  private void checkMemberSelect(MemberSelectExpressionTree memberSelectExpressionTree) {
    checkIdentifier(memberSelectExpressionTree.identifier());
  }

  private void checkMethodInvocation(MethodInvocationTree methodInvocationTree) {
    if (MAP_OF.matches(methodInvocationTree)) {
      for (int i = 0; i < methodInvocationTree.arguments().size(); i += 2) {
        ExpressionTree key = methodInvocationTree.arguments().get(i);
        checkExpression(key, key);
      }
    }
  }

}
