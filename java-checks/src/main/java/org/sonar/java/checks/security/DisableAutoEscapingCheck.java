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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

  private static final String MUSTACHE_ESCAPERS = "com.samskivert.mustache.Escapers";
  private static final String FREEMARKER_CONFIGURATION = "freemarker.template.Configuration";

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
    .ofTypes(MUSTACHE_ESCAPERS)
    .names("simple")
    .withAnyParameters()
    .build();

  private static final MethodMatchers FREEMARKER_SET_AUTO_ESCAPING_POLICY = MethodMatchers.create()
    .ofTypes(FREEMARKER_CONFIGURATION)
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
    if (FREEMARKER_SET_AUTO_ESCAPING_POLICY.matches(mit)) {
      handleFreeMarker(mit);
    } else {
      handleJMustache(mit);
    }
  }

  private void handleFreeMarker(MethodInvocationTree mit) {
    ExpressionTree policy = mit.arguments().get(0);
    if (isFieldFromClassWithName(policy, FREEMARKER_CONFIGURATION, "DISABLE_AUTO_ESCAPING_POLICY")) {
      reportIssue(policy, MESSAGE);
    }
  }

  private void handleJMustache(MethodInvocationTree mit) {
    if (MUSTACHE_COMPILER_ESCAPE_HTML.matches(mit)) {
      ExpressionTree argument = mit.arguments().get(0);
      argument.asConstant(Boolean.class)
        .filter(Boolean.FALSE::equals)
        .ifPresent(cst -> reportIssue(argument, MESSAGE));
    } else if (MUSTACHE_COMPILER_WITH_ESCAPER.matches(mit)) {
      ExpressionTree argument = mit.arguments().get(0);
      if (isSimpleEscaper(argument) || isFieldFromClassWithName(argument, MUSTACHE_ESCAPERS, "NONE")) {
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

  private static boolean isFieldFromClassWithName(Tree tree, String classType, String name) {
    return extractIdentifier(tree)
      .map(identifier -> checkOwner(identifier, classType, name))
      .orElse(false);
  }

  private static Optional<IdentifierTree> extractIdentifier(Tree tree) {
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      return Optional.of(((MemberSelectExpressionTree) tree).identifier());
    } else if (tree.is(Tree.Kind.IDENTIFIER)) {
      return Optional.of((IdentifierTree) tree);
    }
    return Optional.empty();
  }

  private static boolean checkOwner(IdentifierTree identifier, String classType, String name) {
    Symbol owner = identifier.symbol().owner();
    return owner != null
      && owner.type().is(classType)
      && name.equals(identifier.name());
  }

}
