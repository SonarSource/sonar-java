/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Pattern;

@Rule(
  key = "S2068",
  name = "Credentials should not be hard-coded",
  priority = Priority.CRITICAL,
  tags = {Tag.CWE, Tag.OWASP_A2, Tag.SANS_TOP_25_POROUS, Tag.SECURITY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("30min")
public class HardCodedCredentialsCheck extends SubscriptionBaseVisitor {

  private static final Pattern PASSWORD_LITERAL_PATTERN = Pattern.compile("(password|passwd|pwd)=..", Pattern.CASE_INSENSITIVE);
  private static final Pattern PASSWORD_VARIABLE_PATTERN = Pattern.compile("(password|passwd|pwd)", Pattern.CASE_INSENSITIVE);

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.STRING_LITERAL, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      String literalValue = ((LiteralTree) tree).value();
      if (PASSWORD_LITERAL_PATTERN.matcher(literalValue).find()) {
        reportIssue(tree);
      }
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      IdentifierTree simpleName = variable.simpleName();
      if (isStringLiteral(variable.initializer()) && isPasswordVariableName(simpleName)) {
        reportIssue(simpleName);
      }
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignmentExpression = (AssignmentExpressionTree) tree;
      ExpressionTree variable = assignmentExpression.variable();
      if (isStringLiteral(assignmentExpression.expression()) && isPasswordVariable(variable)) {
        reportIssue(variable);
      }
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (isSettingPassword(mit)) {
        reportIssue((mit).methodSelect());
      }
    }
  }

  private static boolean isSettingPassword(MethodInvocationTree tree) {
    List<ExpressionTree> arguments = tree.arguments();
    return arguments.size() == 2 && argumentsAreLiterals(arguments) && isPassword((LiteralTree) arguments.get(0));
  }

  private static boolean isPassword(LiteralTree argument) {
    return argument.is(Tree.Kind.STRING_LITERAL) && PASSWORD_VARIABLE_PATTERN.matcher(LiteralUtils.trimQuotes(argument.value())).matches();
  }

  private static boolean argumentsAreLiterals(List<ExpressionTree> arguments) {
    for (ExpressionTree argument : arguments) {
      if (!argument.is(
        Kind.INT_LITERAL,
        Kind.LONG_LITERAL,
        Kind.FLOAT_LITERAL,
        Kind.DOUBLE_LITERAL,
        Kind.BOOLEAN_LITERAL,
        Kind.CHAR_LITERAL,
        Kind.STRING_LITERAL,
        Kind.NULL_LITERAL)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isStringLiteral(@Nullable ExpressionTree initializer) {
    return initializer != null && initializer.is(Tree.Kind.STRING_LITERAL);
  }

  private static boolean isPasswordVariableName(IdentifierTree identifierTree) {
    return PASSWORD_VARIABLE_PATTERN.matcher(identifierTree.name()).find();
  }

  private static boolean isPasswordVariable(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return isPasswordVariableName(((MemberSelectExpressionTree) variable).identifier());
    } else if (variable.is(Tree.Kind.IDENTIFIER)) {
      return isPasswordVariableName((IdentifierTree) variable);
    }
    return false;
  }

  private void reportIssue(Tree tree) {
    reportIssue(tree, "Remove this hard-coded password.");
  }

}
