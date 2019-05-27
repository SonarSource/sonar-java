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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

@Rule(key = "S4499")
public class SMTPSSLServerIdentityCheck extends AbstractMethodDetection {

  private static final String APACHE_EMAIL = "org.apache.commons.mail.Email";
  private static final String BOOLEAN = "boolean";
  private static final String HASHTABLE = "java.util.Hashtable";

  private static final Set<String> ENABLING_SSL_METHOD_NAMES = new HashSet<>(Arrays.asList(
    "setSSL",
    "setSSLOnConnect",
    "setTLS",
    "setStartTLSEnabled",
    "setStartTLSRequired"
  ));

  private static final MethodMatcher ENABLING_SSL_METHODS = MethodMatcher.create()
    .typeDefinition(TypeCriteria.is(APACHE_EMAIL))
    .name(ENABLING_SSL_METHOD_NAMES::contains)
    .addParameter(BOOLEAN);

  private static final MethodMatcher HASHTABLE_PUT = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf(HASHTABLE))
    .name("put")
    .withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(ENABLING_SSL_METHODS, HASHTABLE_PUT);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree method = ExpressionUtils.getEnclosingMethod(mit);
    if (method != null) {
      Arguments args = mit.arguments();
      if (ENABLING_SSL_METHODS.matches(mit) && LiteralUtils.isTrue(args.get(0))) {
        MethodBodyApacheVisitor apacheVisitor = new MethodBodyApacheVisitor();
        method.accept(apacheVisitor);
        if (!apacheVisitor.isSecured) {
          reportIssue(mit, "Enable server identity validation on this SMTP SSL connection.");
        }
      } else if (HASHTABLE_PUT.matches(mit) && "mail.smtp.socketFactory.class".equals(ExpressionsHelper.getConstantValueAsString(args.get(0)).value())
        && "javax.net.ssl.SSLSocketFactory".equals(ExpressionsHelper.getConstantValueAsString(args.get(1)).value())) {
        MethodBodyHashtableVisitor hashVisitor = new MethodBodyHashtableVisitor();
        method.accept(hashVisitor);
        if (!hashVisitor.isSecured) {
          reportIssue(mit, "Enable server identity validation, set \"mail.smtp.ssl.checkserveridentity\" to true");
        }
      }
    }
    super.onMethodInvocationFound(mit);
  }

  private static boolean isNotFalse(ExpressionTree expression) {
    return !LiteralUtils.isFalse(expression);
  }

  private static class MethodBodyHashtableVisitor extends BaseTreeVisitor {
    private boolean isSecured = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      Arguments args = mit.arguments();
      if (HASHTABLE_PUT.matches(mit)
        && "mail.smtp.ssl.checkserveridentity".equals(ExpressionsHelper.getConstantValueAsString(args.get(0)).value())
        && isNotFalse(args.get(1))) {
        this.isSecured = true;
      }
      super.visitMethodInvocation(mit);
    }
  }
  private static class MethodBodyApacheVisitor extends BaseTreeVisitor {

    private boolean isSecured = false;

    private static final MethodMatcher SET_SSL_CHECK_SERVER_ID = MethodMatcher.create()
      .typeDefinition(APACHE_EMAIL)
      .name("setSSLCheckServerIdentity")
      .addParameter(BOOLEAN);

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (SET_SSL_CHECK_SERVER_ID.matches(mit) && (isNotFalse(mit.arguments().get(0)))) {
        this.isSecured = true;
      }
      super.visitMethodInvocation(mit);
    }
  }
}
