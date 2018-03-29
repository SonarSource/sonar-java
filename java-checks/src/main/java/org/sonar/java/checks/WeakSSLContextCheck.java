/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.LiteralUtils.trimQuotes;

@Rule(key = "S4423")
public class WeakSSLContextCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> STRONG_PROTOCOLS = new HashSet<>(Arrays.asList("TLSv1.2", "DTLSv1.2"));

  private static final MethodMatcher SSLCONTEXT_GETINSTANCE_MATCHER = MethodMatcher.create()
    .typeDefinition("javax.net.ssl.SSLContext")
    .name("getInstance")
    .withAnyParameters();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      Arguments arguments = mit.arguments();
      if (SSLCONTEXT_GETINSTANCE_MATCHER.matches(mit)) {
        ExpressionTree firstArgument = arguments.get(0);
        if (firstArgument.is(Tree.Kind.STRING_LITERAL) && !STRONG_PROTOCOLS.contains(trimQuotes(((LiteralTree) firstArgument).value()))) {
          reportIssue(firstArgument, "Change this code to use a stronger protocol.");
        }
      }
    }
  }


}
