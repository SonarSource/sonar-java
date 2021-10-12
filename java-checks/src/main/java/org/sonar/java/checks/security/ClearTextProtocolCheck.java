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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S5332")
public class ClearTextProtocolCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, Protocol> PROTOCOLS = new HashMap<>();

  private static final String MESSAGE = "Using %s protocol is insecure. Use %s instead.";
  private static final String MESSAGE_HTTP = "Using HTTP protocol is insecure. Use HTTPS instead.";
  private static final String MESSAGE_ANDROID_MIXED_CONTENT = "Using a relaxed mixed content policy is security-sensitive.";

  static {
    PROTOCOLS.put("org.apache.commons.net.ftp.FTPClient", new Protocol("FTP", "SFTP, SCP or FTPS"));
    PROTOCOLS.put("org.apache.commons.net.smtp.SMTPClient", new Protocol("clear-text SMTP", "SMTP over SSL/TLS or SMTP with STARTTLS"));
    PROTOCOLS.put("org.apache.commons.net.telnet.TelnetClient", new Protocol("Telnet", "SSH"));
  }

  private static final MethodMatchers UNSECURE_CLIENTS = MethodMatchers.create()
    .ofTypes(PROTOCOLS.keySet().toArray(new String[0]))
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers OK_HTTP_CONNECTION_SPEC_BUILDERS = MethodMatchers.create()
    .ofTypes("okhttp3.ConnectionSpec$Builder")
    .constructor()
    .addParametersMatcher("okhttp3.ConnectionSpec")
    .build();

  private static final MethodMatchers OK_HTTP_BUILDERS = MethodMatchers.create()
    .ofTypes("okhttp3.OkHttpClient$Builder")
    .names("connectionSpecs")
    .addParametersMatcher(ANY)
    .build();

  private static final MethodMatchers ANDROID_SET_MIXED_CONTENT_MODE = MethodMatchers.create()
    .ofSubTypes("android.webkit.WebSettings")
    .names("setMixedContentMode")
    .addParametersMatcher("int")
    .build();

  public static final Integer MIXED_CONTENT_ALWAYS_ALLOW = 0;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) tree;
      if (UNSECURE_CLIENTS.matches(newClassTree)) {
        Protocol protocol = PROTOCOLS.get(newClassTree.symbolType().fullyQualifiedName());
        String message = String.format(MESSAGE, protocol.protocolName, protocol.alternatives);
        reportIssue(newClassTree.identifier(), message);
      } else if (OK_HTTP_CONNECTION_SPEC_BUILDERS.matches(newClassTree)) {
        reportIfUsesClearText(newClassTree.arguments());
      }
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (OK_HTTP_BUILDERS.matches(mit)) {
        reportIfUsesClearText(mit.arguments());
      } else if (ANDROID_SET_MIXED_CONTENT_MODE.matches(mit)) {
        reportIfAlwaysAllow(mit.arguments().get(0));
      }
    }
  }

  private void reportIfAlwaysAllow(ExpressionTree setMixedContentModeArgument) {
    if (MIXED_CONTENT_ALWAYS_ALLOW.equals(ExpressionUtils.resolveAsConstant(setMixedContentModeArgument))) {
      reportIssue(setMixedContentModeArgument, MESSAGE_ANDROID_MIXED_CONTENT);
    }
  }

  private void reportIfUsesClearText(Arguments arguments) {
    arguments.accept(new ClearTextVisitor());
  }

  class ClearTextVisitor extends BaseTreeVisitor {
    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if ("CLEARTEXT".equals(tree.name())) {
        reportIssue(tree, MESSAGE_HTTP);
      }
    }
  }

  private static class Protocol {
    String protocolName;
    String alternatives;

    Protocol(String protocolName, String alternatives) {
      this.protocolName = protocolName;
      this.alternatives = alternatives;
    }
  }
}
