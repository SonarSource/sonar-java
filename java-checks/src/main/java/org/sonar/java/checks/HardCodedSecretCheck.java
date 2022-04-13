/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.RandomnessDetector;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.HardcodedIpCheck.IP_V6_ALONE;

@Rule(key = "S6418")
public class HardCodedSecretCheck extends AbstractHardCodedCredentialChecker {

  private static final String DEFAULT_SECRET_WORDS = "api[_.-]?key,auth,credential,secret,token";
  private static final String DEFAULT_RANDOMNESS_SENSIBILITY= "5.0";
  private static final int MINIMUM_CREDENTIAL_LENGTH = 17;

  private static final String FIRST_ACCEPTED_CHARACTER = "[\\w.+/~$:&-]";
  private static final String FOLLOWING_ACCEPTED_CHARACTER = "[=\\w.+/~$:&-]";
  private static final Pattern SECRET_PATTERN =
    Pattern.compile(FIRST_ACCEPTED_CHARACTER + "(" + FOLLOWING_ACCEPTED_CHARACTER + "|\\\\\\\\" + FOLLOWING_ACCEPTED_CHARACTER + ")++");
  private static final Pattern IPV_6_PATTERN = Pattern.compile(IP_V6_ALONE);

  private RandomnessDetector randomnessDetector;

  @RuleProperty(
    key = "secretWords",
    description = "Comma separated list of words identifying potential secrets",
    defaultValue = DEFAULT_SECRET_WORDS)
  public String secretWords = DEFAULT_SECRET_WORDS;

  @RuleProperty(
    key = "randomnessSensibility",
    description = "Allows to tune the Randomness Sensibility (from 0 to 10)",
    defaultValue = DEFAULT_RANDOMNESS_SENSIBILITY)
  public double randomnessSensibility = Double.parseDouble(DEFAULT_RANDOMNESS_SENSIBILITY);

  @Override
  protected String getCredentialWords() {
    return secretWords;
  }

  @Override
  protected boolean isCredentialContainingPattern(ExpressionTree expression) {
    // Secrets containing a secret word is not considered as containing an expression.
    // Simple constant declaration like "String secret = "secret"" will anyway be filtered by the entropy filter.
    return false;
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.STRING_LITERAL, Kind.VARIABLE, Kind.ASSIGNMENT, Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.STRING_LITERAL)) {
      handleStringLiteral((LiteralTree) tree);
    } else if (tree.is(Kind.VARIABLE)) {
      handleVariable((VariableTree) tree);
    } else if (tree.is(Kind.ASSIGNMENT)) {
      handleAssignment((AssignmentExpressionTree) tree);
    } else {
      handleMethodInvocation((MethodInvocationTree) tree);
    }
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
  protected boolean isPotentialCredential(String literal) {
    if (literal.length() < MINIMUM_CREDENTIAL_LENGTH || !SECRET_PATTERN.matcher(literal).matches()) {
      return false;
    }
    return getRandomnessDetector().isRandom(literal)
      && isNotIpV6(literal);
  }

  private RandomnessDetector getRandomnessDetector() {
    if (randomnessDetector == null) {
      randomnessDetector = new RandomnessDetector(randomnessSensibility);
    }
    return randomnessDetector;
  }

  private static boolean isNotIpV6(String literal) {
    return !IPV_6_PATTERN.matcher(literal).matches();
  }

  @Override
  protected void report(Tree tree, String match) {
    reportIssue(tree, "'" + match + "' detected in this expression, review this potentially hard-coded secret.");
  }

}
