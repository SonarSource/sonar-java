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

  private static final String EMAIL_CLASS_NAME = "org.apache.commons.mail.Email";
  private static final String SESSION_CLASS_NAME = "javax.mail.Session";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.is(EMAIL_CLASS_NAME))
        .name("send")
        .withoutParameter(),
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.is(SESSION_CLASS_NAME))
        .name("getDefaultInstance")
        .withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree method = findEnclosingMethod(mit);
    MethodBodyVisitor methodVisitor = null;
    if (method != null) {
      if ("send".equals(mit.symbol().name())) {
        methodVisitor = new MethodBodyVisitor(0);
      } else {
        methodVisitor = new MethodBodyVisitor(1);
      }
      method.accept(methodVisitor);
      if (methodVisitor.bothFunctionsAreChecked < 2) {
        if (methodVisitor.differentiateEmailAndSessionInvocations == 0) {
          reportIssue(mit.methodSelect(), "Enable server identity validation on this SMTP SSL connection.");
        } else if (methodVisitor.differentiateEmailAndSessionInvocations == 1) {
          reportIssue(mit.methodSelect(), "Enable server identity validation, set \"mail.smtp.ssl.checkserveridentity\" to true");
        }
      }
    }
    super.onMethodInvocationFound(mit);
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

  private static class MethodBodyVisitor extends BaseTreeVisitor {

    private Integer bothFunctionsAreChecked = 0;
    /*
     * MethodVisitor constructor's parameter is 0 when "send()" from org.apache.commons.mail.Email is invoked
     * and 1 for "getDefaultInstance" from javax.mail.Session
     */
    private final Integer differentiateEmailAndSessionInvocations;

    private static final String BOOLEAN = "boolean";
    private static final String HASHTABLE = "java.util.Hashtable";

    private static final MethodMatcher SET_SSL_ON_CONNECT = MethodMatcher.create()
      .typeDefinition(TypeCriteria.is(EMAIL_CLASS_NAME))
      .name("setSSLOnConnect")
      .addParameter(BOOLEAN);

    private static final MethodMatcher SET_SSL_CHECK_SERVER_ID = MethodMatcher.create()
      .typeDefinition(EMAIL_CLASS_NAME)
      .name("setSSLCheckServerIdentity")
      .addParameter(BOOLEAN);

    private static final MethodMatcher MAIL_SMTP_SSL = MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(HASHTABLE))
      .name("put")
      .withAnyParameters();

    public MethodBodyVisitor(Integer specifyInvocationByNumber) {
      differentiateEmailAndSessionInvocations = specifyInvocationByNumber;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      Arguments args = mit.arguments();
      if (differentiateEmailAndSessionInvocations == 0
        && (SET_SSL_CHECK_SERVER_ID.matches(mit) || SET_SSL_ON_CONNECT.matches(mit))
        && LiteralUtils.isTrue(args.get(0))) {
        bothFunctionsAreChecked++;
      }

      if (differentiateEmailAndSessionInvocations == 1 && MAIL_SMTP_SSL.matches(mit)) {
        ExpressionTree arg1 = args.get(0);
        ExpressionTree arg2 = args.get(1);
        if (mailSessionIsChecked(arg1, arg2) || parametersSocketFactoryMatch(arg1, arg2)) {
          bothFunctionsAreChecked++;
        }
      }
      super.visitMethodInvocation(mit);
    }

    private static boolean mailSessionIsChecked(ExpressionTree arg1, ExpressionTree arg2) {
      return ("mail.smtp.ssl.checkserveridentity".equals(ConstantUtils.resolveAsStringConstant(arg1))
        && LiteralUtils.isTrue(arg2));
    }

    private static boolean parametersSocketFactoryMatch(ExpressionTree arg1, ExpressionTree arg2) {
      return "mail.smtp.socketFactory.class".equals(ConstantUtils.resolveAsStringConstant(arg1))
        && "javax.net.ssl.SSLSocketFactory".equals(ConstantUtils.resolveAsStringConstant(arg2));
    }
  }
}
