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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5344")
public class PasswordEncoderCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers JDBC_AUTHENTICATION = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder")
    .names("jdbcAuthentication")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers USER_DETAIL_SERVICE = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder")
    .names("userDetailsService")
    .withAnyParameters()
    .build();

  private static final MethodMatchers PASSWORD_ENCODER_SETTER = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.authentication.configurers.userdetails.AbstractDaoAuthenticationConfigurer")
    .names("passwordEncoder")
    .withAnyParameters()
    .build();

  private static final MethodMatchers UNSAFE_PASSWORD_ENCODER_CONSTRUCTORS = MethodMatchers.create()
    .ofTypes(
      "org.springframework.security.authentication.encoding.ShaPasswordEncoder",
      "org.springframework.security.authentication.encoding.Md5PasswordEncoder",
      "org.springframework.security.crypto.password.LdapShaPasswordEncoder",
      "org.springframework.security.crypto.password.Md4PasswordEncoder",
      "org.springframework.security.crypto.password.MessageDigestPasswordEncoder",
      "org.springframework.security.crypto.password.StandardPasswordEncoder",
      "org.springframework.security.crypto.scrypt.SCryptPasswordEncoder")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers UNSAFE_PASSWORD_ENCODER_METHODS = MethodMatchers.create()
    .ofTypes("org.springframework.security.crypto.password.NoOpPasswordEncoder")
    .names("getInstance")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof NewClassTree nct && UNSAFE_PASSWORD_ENCODER_CONSTRUCTORS.matches(nct)) {
      reportIssue(((NewClassTree) tree).identifier(), "Use secure \"PasswordEncoder\" implementation.");
    } else if (tree instanceof MethodInvocationTree mit && UNSAFE_PASSWORD_ENCODER_METHODS.matches(mit)) {
      reportIssue(ExpressionUtils.methodName(mit), "Use secure \"PasswordEncoder\" implementation.");
    } else if (tree.is(Tree.Kind.METHOD)) {
      MethodInvocationVisitor visitor = new MethodInvocationVisitor();
      tree.accept(visitor);
      if (visitor.hasAuthentication && !visitor.setsPasswordEncoder) {
        reportIssue(visitor.tree, "Don't use the default \"PasswordEncoder\" relying on plain-text.");
      }
    }
  }

  static class MethodInvocationVisitor extends BaseTreeVisitor {

    private boolean hasAuthentication;
    private boolean setsPasswordEncoder;
    private MethodInvocationTree tree;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (JDBC_AUTHENTICATION.matches(tree) || USER_DETAIL_SERVICE.matches(tree)) {
        hasAuthentication = true;
        this.tree = tree;
      }
      if (PASSWORD_ENCODER_SETTER.matches(tree)) {
        setsPasswordEncoder = true;
      }
      super.visitMethodInvocation(tree);
    }
  }
}
