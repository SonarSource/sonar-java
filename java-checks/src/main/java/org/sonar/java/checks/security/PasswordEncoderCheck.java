/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5344")
public class PasswordEncoderCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher JDBC_AUTHENTICATION = MethodMatcher.create()
    .typeDefinition("org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder")
    .name("jdbcAuthentication")
    .withoutParameter();

  private static final MethodMatcher USER_DETAIL_SERVICE = MethodMatcher.create()
    .typeDefinition("org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder")
    .name("userDetailsService")
    .withAnyParameters();

  private static final MethodMatcher PASSWORD_ENCODER_SETTER = MethodMatcher.create()
    .typeDefinition("org.springframework.security.config.annotation.authentication.configurers.userdetails.AbstractDaoAuthenticationConfigurer")
    .name("passwordEncoder")
    .withAnyParameters();

  private static final MethodMatcherCollection UNSAFE_PASSWORD_ENCODERS = MethodMatcherCollection.create(
    constructorMatcher("org.springframework.security.authentication.encoding.ShaPasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.authentication.encoding.Md5PasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.crypto.password.LdapShaPasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.crypto.password.Md4PasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.crypto.password.MessageDigestPasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.crypto.password.NoOpPasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.crypto.password.StandardPasswordEncoder").withAnyParameters(),
    constructorMatcher("org.springframework.security.crypto.password.SCryptPasswordEncoder").withAnyParameters()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.NEW_CLASS) && UNSAFE_PASSWORD_ENCODERS.anyMatch(((NewClassTree) tree))) {
      reportIssue(((NewClassTree) tree).identifier(), "Use secure \"PasswordEncoder\" implementation.");
    } else if (tree.is(Tree.Kind.METHOD)) {
      MethodInvocationVisitor visitor = new MethodInvocationVisitor();
      tree.accept(visitor);
      if (visitor.hasAuthentication && !visitor.setsPasswordEncoder) {
        reportIssue(visitor.tree, "Don't use the default \"PasswordEncoder\" relying on plain-text.");
      }
    }
  }

  private static MethodMatcher constructorMatcher(String fullyQualifiedName) {
    return MethodMatcher.create().typeDefinition(fullyQualifiedName).name("<init>");
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
