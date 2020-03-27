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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4834")
public class ControllingPermissionsCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(
    "org.springframework.security.access.prepost.PostAuthorize",
    "org.springframework.security.access.prepost.PostFilter",
    "org.springframework.security.access.prepost.PreAuthorize",
    "org.springframework.security.access.prepost.PreFilter",
    "org.springframework.security.access.annotation.Secured"));

  private static final List<String> JSR_250_ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(
    "javax.annotation.security.RolesAllowed",
    "javax.annotation.security.PermitAll",
    "javax.annotation.security.DenyAll"));

  private static final String ORG_SPRINGFRAMEWORK_SECURITY_CORE_GRANTED_AUTHORITY = "org.springframework.security.core.GrantedAuthority";
  private static final List<String> INTERFACES = Collections.unmodifiableList(Arrays.asList(
    "org.springframework.security.access.AccessDecisionVoter",
    "org.springframework.security.access.AccessDecisionManager",
    "org.springframework.security.access.AfterInvocationProvider",
    "org.springframework.security.access.PermissionEvaluator",
    "org.springframework.security.access.expression.SecurityExpressionOperations",
    "org.springframework.security.access.expression.method.MethodSecurityExpressionHandler",
    ORG_SPRINGFRAMEWORK_SECURITY_CORE_GRANTED_AUTHORITY,
    "org.springframework.security.acls.model.PermissionGrantingStrategy"));

  private static final String GLOBAL_METHOD_SECURITY_CONFIGURATION = "org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration";

  private static final String MUTABLE_ACL_SERVICE = "org.springframework.security.acls.model.MutableAclService";
  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(MUTABLE_ACL_SERVICE).names("createAcl", "deleteAcl", "updateAcl").withAnyParameters().build(),
    MethodMatchers.create()
      .ofTypes("org.springframework.security.config.annotation.web.builders.HttpSecurity").names("authorizeRequests").withAnyParameters().build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE,
      Tree.Kind.NEW_CLASS,
      Tree.Kind.METHOD,
      Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    switch (tree.kind()) {
      case CLASS:
      case ENUM:
      case INTERFACE:
        handleClassTree((ClassTree) tree);
        break;
      case NEW_CLASS:
        handleNewClassTree((NewClassTree) tree);
        break;
      case METHOD:
        handleMethodTree((MethodTree) tree);
        break;
      case METHOD_INVOCATION:
        handleMethodInvocationTree((MethodInvocationTree) tree);
        break;
      default:
        // do nothing - not subscribed
        break;
    }
  }

  private void handleMethodTree(MethodTree tree) {
    ModifiersTree modifiers = tree.modifiers();
    checkAnnotations(modifiers, ANNOTATIONS);
    checkAnnotations(modifiers, JSR_250_ANNOTATIONS);
  }

  private void handleClassTree(ClassTree tree) {
    tree.superInterfaces().stream()
      .filter(superInterface -> INTERFACES.stream().anyMatch(superInterface.symbolType()::is))
      .forEach(this::reportIssue);

    TypeTree superClass = tree.superClass();
    if (superClass != null && superClass.symbolType().is(GLOBAL_METHOD_SECURITY_CONFIGURATION)) {
      reportIssue(superClass);
    }

    checkAnnotations(tree.modifiers(), JSR_250_ANNOTATIONS);
  }

  private void checkAnnotations(ModifiersTree modifiers, List<String> annotations) {
    modifiers.annotations().stream()
      .filter(annotationTree -> annotations.stream().anyMatch(annotationTree.symbolType()::is))
      .forEach(this::reportIssue);
  }

  private void handleNewClassTree(NewClassTree tree) {
    JUtils.directSuperTypes(tree.symbolType()).stream()
      .filter(directSuperType -> isGrantedAuthority(directSuperType) || isForbiddenForAnonymousClass(tree, directSuperType))
      .findFirst()
      .ifPresent(ct -> reportIssue(tree.identifier()));
  }

  private static boolean isGrantedAuthority(Type dst) {
    return dst.is(ORG_SPRINGFRAMEWORK_SECURITY_CORE_GRANTED_AUTHORITY);
  }

  private static boolean isForbiddenForAnonymousClass(NewClassTree tree, Type dst) {
    return tree.classBody() != null && (INTERFACES.stream().anyMatch(dst::is) || dst.is(GLOBAL_METHOD_SECURITY_CONFIGURATION));
  }

  private void handleMethodInvocationTree(MethodInvocationTree tree) {
    if (METHOD_MATCHERS.matches(tree)) {
      reportIssue(ExpressionUtils.methodName(tree));
    }
  }

  private void reportIssue(Tree tree) {
    reportIssue(tree, "Make sure that Permissions are controlled safely here.");
  }
}
