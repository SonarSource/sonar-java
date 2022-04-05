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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ShannonEntropy;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6418")
public class HardCodedSecretCheck extends AbstractHardCodedCredentialChecker {

  private static final String DEFAULT_SECRET_WORDS = "secret,token,api[_.-]?key,credential,auth";

  @RuleProperty(
    key = "secretWords",
    description = "Comma separated list of words identifying potential secrets",
    defaultValue = DEFAULT_SECRET_WORDS)
  public String secretWords = DEFAULT_SECRET_WORDS;

  @Override
  protected String getCredentialWords() {
    return secretWords;
  }

  @Override
  protected int minCredentialLength() {
    return 2;
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
    isSettingCredential(mit).ifPresent(settingSecret -> report(ExpressionUtils.methodName(mit), settingSecret));
  }

  @Override
  protected boolean isPotentialCredential(@Nullable String literal) {
    return super.isPotentialCredential(literal) && ShannonEntropy.hasHighEntropy(literal);
  }

  protected void report(Tree tree, String match) {
    reportIssue(tree, "'" + match + "' detected in this expression, review this potentially hard-coded secret.");
  }

}
