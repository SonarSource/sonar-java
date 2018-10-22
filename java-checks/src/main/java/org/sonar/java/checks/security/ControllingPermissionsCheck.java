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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4834")
public class ControllingPermissionsCheck extends BaseTreeVisitor implements JavaFileScanner {
  private static final String MUTABLE_ACL_SERVICE = "org.springframework.security.acls.model.MutableAclService";

  private static final String MESSAGE = "Make sure that Permissions are controlled safely here.";

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

  private static final List<String> INTERFACES = Collections.unmodifiableList(Arrays.asList(
    "org.springframework.security.access.AccessDecisionVoter",
    "org.springframework.security.access.AccessDecisionManager",
    "org.springframework.security.access.AfterInvocationProvider",
    "org.springframework.security.access.PermissionEvaluator",
    "org.springframework.security.access.expression.SecurityExpressionOperations",
    "org.springframework.security.access.expression.method.MethodSecurityExpressionHandler",
    "org.springframework.security.core.GrantedAuthority",
    "org.springframework.security.acls.model.PermissionGrantingStrategy"));

  private static final List<String> CLASSES = Collections.singletonList("org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration");

  private static final MethodMatcherCollection METHOD_MATCHERS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(MUTABLE_ACL_SERVICE).name("createAcl").withAnyParameters(),
    MethodMatcher.create().typeDefinition(MUTABLE_ACL_SERVICE).name("deleteAcl").withAnyParameters(),
    MethodMatcher.create().typeDefinition(MUTABLE_ACL_SERVICE).name("updateAcl").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.springframework.security.config.annotation.web.builders.HttpSecurity").name("authorizeRequests").withAnyParameters());

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      return;
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (!tree.symbol().isInterface()) {
      tree.superInterfaces().forEach(superInterface -> {
        Type interfaceType = superInterface.symbolType();
        if (INTERFACES.stream().anyMatch(interfaceType::is)) {
          context.reportIssue(this, superInterface, MESSAGE);
        }
      });
      TypeTree superClass = tree.superClass();
      if (superClass != null && CLASSES.stream().anyMatch(superClass.symbolType()::is)) {
        context.reportIssue(this, superClass, MESSAGE);
      }
    }
    tree.modifiers().annotations().forEach(annotationTree -> {
      Type annotationType = annotationTree.symbolType();
      if (JSR_250_ANNOTATIONS.stream().anyMatch(annotationType::is)) {
        context.reportIssue(this, annotationTree, MESSAGE);
      }
    });
    super.visitClass(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.classBody() != null) {
      Type newClassType = tree.symbolType();
      // only target anonymous classes
      if (INTERFACES.stream().anyMatch(newClassType::isSubtypeOf)
        || CLASSES.stream().anyMatch(newClassType::isSubtypeOf)) {
        context.reportIssue(this, tree.newKeyword(), tree.arguments(), MESSAGE);
      }
    }
    super.visitNewClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    tree.modifiers().annotations().forEach(annotationTree -> {
      Type annotationType = annotationTree.symbolType();
      if (Stream.concat(ANNOTATIONS.stream(), JSR_250_ANNOTATIONS.stream()).anyMatch(annotationType::is)) {
        context.reportIssue(this, annotationTree, MESSAGE);
      }
    });
    super.visitMethod(tree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (METHOD_MATCHERS.anyMatch(tree)) {
      context.reportIssue(this, ExpressionUtils.methodName(tree), tree.arguments(), MESSAGE);
    }
    super.visitMethodInvocation(tree);
  }
}
