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
package org.sonar.java.checks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2068")
public class HardCodedPasswordCheck extends AbstractHardCodedCredentialChecker {

  private static final String DEFAULT_PASSWORD_WORDS = "password,passwd,pwd,passphrase,java.naming.security.credentials";
  private static final Pattern URL_PREFIX = Pattern.compile("^\\w{1,8}://");
  private static final Pattern NON_EMPTY_URL_CREDENTIAL = Pattern.compile("(?<user>[^\\s:]*+):(?<password>\\S++)");

  @RuleProperty(
    key = "credentialWords",
    description = "Comma separated list of words identifying potential passwords",
    defaultValue = DEFAULT_PASSWORD_WORDS)
  public String passwordWords = DEFAULT_PASSWORD_WORDS;

  @Override
  protected String getCredentialWords() {
    return passwordWords;
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.STRING_LITERAL, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      handleStringLiteral((LiteralTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      handleVariable((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      handleAssignment((AssignmentExpressionTree) tree);
    } else {
      handleMethodInvocation((MethodInvocationTree) tree);
    }
  }

  @Override
  protected void handleStringLiteral(LiteralTree tree) {
    String cleanedLiteral = LiteralUtils.trimQuotes(tree.value());
    if (isURLWithCredentials(cleanedLiteral)) {
      reportIssue(tree, "Review this hard-coded URL, which may contain a password.");
    } else {
      super.handleStringLiteral(tree);
    }
  }

  private static boolean isURLWithCredentials(String stringLiteral) {
    if (URL_PREFIX.matcher(stringLiteral).find()) {
      try {
        String userInfo = new URL(stringLiteral).getUserInfo();
        if (userInfo != null) {
          Matcher matcher = NON_EMPTY_URL_CREDENTIAL.matcher(userInfo);
          return matcher.matches() && !matcher.group("user").equals(matcher.group("password"));
        }
      } catch (MalformedURLException e) {
        // ignore, stringLiteral is not a valid URL
      }
    }
    return false;
  }

  private void handleMethodInvocation(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (EQUALS_MATCHER.matches(mit) && methodSelect.is(Kind.MEMBER_SELECT)) {
      handleEqualsMethod(mit, (MemberSelectExpressionTree) methodSelect);
    } else {
      isSettingCredential(mit).ifPresent(settingPassword -> report(ExpressionUtils.methodName(mit), settingPassword));
    }
  }

  @Override
  protected void report(Tree tree, String match) {
    reportIssue(tree, "'" + match + "' detected in this expression, review this potentially hard-coded password.");
  }

  @Override
  protected boolean isCredentialContainingPattern(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      ExpressionTree methodSelect = ((MethodInvocationTree) expression).methodSelect();
      return methodSelect.is(Tree.Kind.MEMBER_SELECT) && isCredentialContainingPattern(((MemberSelectExpressionTree) methodSelect).expression());
    }
    String literal = ExpressionsHelper.getConstantValueAsString(expression).value();
    return literal == null || super.isCredentialLikeName(literal).isPresent();
  }
}
