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

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4499")
public class SMTPSSLServerIdentityCheck extends AbstractMethodDetection {

  private static final String APACHE = "org.apache.commons.mail.Email";
  private static final String BOOLEAN = "boolean";
  private static final String HASHTABLE = "java.util.Hashtable";

  private static final MethodMatcher SET_SSL_ON_CONNECT = MethodMatcher.create()
    .typeDefinition(TypeCriteria.is(APACHE))
    .name("setSSLOnConnect")
    .addParameter(BOOLEAN);

  private static final MethodMatcher HASHTABLE_PUT = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf(HASHTABLE))
    .name("put")
    .withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(SET_SSL_ON_CONNECT, HASHTABLE_PUT);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree method = findEnclosingMethod(mit);
    MethodBodyVisitor methodVisitor = null;
    if (method != null) {
      Arguments args = mit.arguments();
      if (SET_SSL_ON_CONNECT.matches(mit) && LiteralUtils.isTrue(args.get(0))) {
        methodVisitor = new MethodBodyVisitor(0);
      } else if (HASHTABLE_PUT.matches(mit) && parametersSocketFactoryMatch(args.get(0), args.get(1))) {
        methodVisitor = new MethodBodyVisitor(1);
      }
      if (methodVisitor != null) {
        visitEnclosingMethodAndReportMessageIfNotSecure(method, mit, methodVisitor);
      }
    }
    super.onMethodInvocationFound(mit);
  }

  private void visitEnclosingMethodAndReportMessageIfNotSecure(MethodTree method, MethodInvocationTree mit, MethodBodyVisitor methodVisitor) {
    method.accept(methodVisitor);
    if (!methodVisitor.isSecured) {
      if (methodVisitor.differentiateInvocations == 0) {
        reportIssue(mit, "Enable server identity validation on this SMTP SSL connection.");
      } else if (methodVisitor.differentiateInvocations == 1) {
        reportIssue(mit, "Enable server identity validation, set \"mail.smtp.ssl.checkserveridentity\" to true");
      }
    }
  }

  @CheckForNull
  private static MethodTree findEnclosingMethod(Tree tree) {
    while (!tree.is(Tree.Kind.CLASS, Tree.Kind.METHOD)) {
      tree = tree.parent();
    }
    if (tree.is(Tree.Kind.CLASS)) {
      return null;
    }
    return (MethodTree) tree;
  }

  private static boolean parametersSocketFactoryMatch(ExpressionTree arg1, ExpressionTree arg2) {
    return "mail.smtp.socketFactory.class".equals(ConstantUtils.resolveAsStringConstant(arg1))
      && "javax.net.ssl.SSLSocketFactory".equals(ConstantUtils.resolveAsStringConstant(arg2));
  }

  private static class MethodBodyVisitor extends BaseTreeVisitor {

    /*
     * MethodVisitor constructor's parameter is 0 when "send()" from org.apache.commons.mail.Email is invoked
     * and 1 for "getDefaultInstance" from javax.mail.Session
     */
    private final Integer differentiateInvocations;
    private boolean isSecured = false;
    private static final MethodMatcher SET_SSL_CHECK_SERVER_ID = MethodMatcher.create()
      .typeDefinition(APACHE)
      .name("setSSLCheckServerIdentity")
      .addParameter(BOOLEAN);

    public MethodBodyVisitor(Integer specifyInvocationByNumber) {
      differentiateInvocations = specifyInvocationByNumber;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      Arguments args = mit.arguments();
      if (differentiateInvocations == 0 && SET_SSL_CHECK_SERVER_ID.matches(mit)
        && (argIsNotBooleanOrIsTrue(args.get(0)))) {
        this.isSecured = true;
      }

      if (differentiateInvocations == 1 && HASHTABLE_PUT.matches(mit)
        && mailSessionIsChecked(args.get(0), args.get(1))) {
        this.isSecured = true;
      }
      super.visitMethodInvocation(mit);
    }

    private static boolean mailSessionIsChecked(ExpressionTree arg1, ExpressionTree arg2) {
      return ("mail.smtp.ssl.checkserveridentity".equals(ConstantUtils.resolveAsStringConstant(arg1))
        && argIsNotBooleanOrIsTrue(arg2));
    }

    private static boolean argIsNotBooleanOrIsTrue(ExpressionTree arg) {
      return LiteralUtils.isTrue(arg) || !arg.is(Tree.Kind.BOOLEAN_LITERAL);
    }
  }
}
