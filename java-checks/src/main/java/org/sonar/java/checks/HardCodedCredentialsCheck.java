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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;
import java.util.regex.Pattern;

@Rule(
  key = "S2068",
  priority = Priority.CRITICAL,
  tags = {"cwe", "owasp-top10", "sans-top25-2011", "security"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class HardCodedCredentialsCheck extends SubscriptionBaseVisitor {

  private static final Pattern PASSWORD_LITERAL_PATTERN = Pattern.compile("password=..", Pattern.CASE_INSENSITIVE);
  private static final Pattern PASSWORD_VARIABLE_PATTERN = Pattern.compile("password", Pattern.CASE_INSENSITIVE);

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.STRING_LITERAL, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      String literalValue = ((LiteralTree) tree).value();
      if (PASSWORD_LITERAL_PATTERN.matcher(literalValue).find()) {
        addIssue(tree);
      }
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      if (isStringLiteral(variable.initializer()) && isPasswordVariableName(variable.simpleName())) {
        addIssue(tree);
      }
    } else {
      AssignmentExpressionTree assignmentExpression = (AssignmentExpressionTree) tree;
      if (isStringLiteral(assignmentExpression.expression()) && isPasswordVariable(assignmentExpression.variable())) {
        addIssue(tree);
      }
    }
  }

  private boolean isStringLiteral(ExpressionTree initializer) {
    return initializer != null && initializer.is(Tree.Kind.STRING_LITERAL) && !isEmptyStringLiteral((LiteralTree) initializer);
  }

  private boolean isEmptyStringLiteral(LiteralTree initializer){
	  return "\"\"".equals(initializer.value());
  }
  
  private boolean isPasswordVariableName(IdentifierTree identifierTree) {
    return PASSWORD_VARIABLE_PATTERN.matcher(identifierTree.name()).find();
  }

  private boolean isPasswordVariable(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return isPasswordVariableName(((MemberSelectExpressionTree) variable).identifier());
    } else if (variable.is(Tree.Kind.IDENTIFIER)) {
      return isPasswordVariableName((IdentifierTree) variable);
    }
    return false;
  }

  private void addIssue(Tree tree) {
    addIssue(tree, "Remove this hard-coded password.");
  }

}
