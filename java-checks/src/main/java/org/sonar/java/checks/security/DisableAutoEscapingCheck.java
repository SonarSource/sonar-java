/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5247")
public class DisableAutoEscapingCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure disabling auto-escaping feature is safe here.";

  private static final MethodMatchers MUSTACHE_COMPILER_ESCAPE_HTML = MethodMatchers.create()
    .ofTypes("com.samskivert.mustache.Mustache$Compiler")
    .names("escapeHTML")
    .addParametersMatcher("boolean")
    .build();

  private static final MethodMatchers MUSTACHE_COMPILER_WITH_ESCAPER = MethodMatchers.create()
    .ofTypes("com.samskivert.mustache.Mustache$Compiler")
    .names("withEscaper")
    .addParametersMatcher("com.samskivert.mustache.Mustache$Escaper")
    .build();

  private static final MethodMatchers MUSTACHE_ESCAPERS_SIMPLE = MethodMatchers.create()
    .ofTypes("com.samskivert.mustache.Escapers")
    .names("simple")
    .withAnyParameters()
    .build();

  private static final MethodMatchers FREEMARKER_SET_AUTO_ESCAPING_POLICY = MethodMatchers.create()
    .ofTypes("freemarker.template.Configuration")
    .names("setAutoEscapingPolicy")
    .addParametersMatcher("int")
    .build();


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    handleJMustache(mit);
    handleFreeMarker(mit);
  }

  private void handleJMustache(MethodInvocationTree mit) {
    if (MUSTACHE_COMPILER_ESCAPE_HTML.matches(mit)) {
      ExpressionTree argument = mit.arguments().get(0);
      argument.asConstant(Boolean.class)
        .filter(Boolean.FALSE::equals)
        .ifPresent(cst -> reportIssue(argument, MESSAGE));
    } else if (MUSTACHE_COMPILER_WITH_ESCAPER.matches(mit)) {
      ExpressionTree argument = mit.arguments().get(0);
      if (isSimpleEscaper(argument) || isFieldFromClassWithName(argument, "com.samskivert.mustache.Escapers", "NONE")) {
        reportIssue(argument, MESSAGE);
      }
    }
  }

  private static boolean isSimpleEscaper(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      // Escapers.simple has a varargs argument, we have to rely on the list of arguments and cannot use the matcher.
      return mit.arguments().isEmpty() && MUSTACHE_ESCAPERS_SIMPLE.matches(mit);
    }
    return false;
  }

  private void handleFreeMarker(MethodInvocationTree mit) {
    if (FREEMARKER_SET_AUTO_ESCAPING_POLICY.matches(mit)) {
      ExpressionTree policy = mit.arguments().get(0);
      if (isFieldFromClassWithName(policy, "freemarker.template.Configuration", "DISABLE_AUTO_ESCAPING_POLICY")) {
        reportIssue(policy, MESSAGE);
      }
    }
  }

  private static boolean isFieldFromClassWithName(Tree tree, String classType, String name) {
    IdentifierTree identifier = null;
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      identifier = ((MemberSelectExpressionTree) tree).identifier();
    } else if (tree.is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) tree;
    }
    if (identifier != null) {
      Symbol owner = identifier.symbol().owner();
      return owner != null
        && owner.type().is(classType)
        && name.equals(identifier.name());
    }
    return false;
  }

}
