/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.regex;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class AbstractRegexCheck extends AbstractMethodDetection implements RegexCheck {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers REGEX_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("matches")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("replaceAll", "replaceFirst")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.util.regex.Pattern")
      .names("compile", "matches")
      .withAnyParameters()
      .build());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return REGEX_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments args = mit.arguments();
    if (args.isEmpty()) {
      return;
    }
    ExpressionTree arg0 = args.get(0);
    if (arg0.is(Tree.Kind.STRING_LITERAL)) {
      checkRegex(((DefaultJavaFileScannerContext) context).regexForLiterals((LiteralTree) arg0), mit);
    }
  }

  public abstract void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit);

  public final void reportIssue(RegexSyntaxElement regexTree, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    ((DefaultJavaFileScannerContext) context).reportIssue(this, regexTree, message, cost, secondaries);
  }

}
