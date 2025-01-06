/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5689")
public class DisclosingTechnologyFingerprintsCheck extends IssuableSubscriptionVisitor {
  
  private static final String MESSAGE = "Make sure disclosing version information of this web technology is safe here.";

  public static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers SET_RESPONSE_HEADERS = MethodMatchers.or(
    MethodMatchers.create()
    .ofSubTypes("javax.servlet.http.HttpServletResponse", 
      "javax.servlet.http.HttpServletResponseWrapper", 
      "org.apache.wicket.request.http.WebResponse",
      "org.apache.wicket.protocol.http.BufferedWebResponse", 
      "org.apache.wicket.protocol.http.servlet.ServletWebResponse")
    .names("addHeader", "setHeader")
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
    .build(),
    MethodMatchers.create()
      .ofSubTypes("org.springframework.http.HttpHeaders")
      .names("add", "set")
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.rapidoid.http.Resp")
      .names("header")
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.springframework.http.ResponseEntity$HeadersBuilder")
      .names("header")
      .withAnyParameters()
      .build()
  );
  
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    if (SET_RESPONSE_HEADERS.matches(methodInvocationTree)) {
      methodInvocationTree.arguments().get(0).asConstant(String.class)
        .ifPresent(header -> {
          if ("server".equalsIgnoreCase(header) ||
            "x-powered-by".equalsIgnoreCase(header)) {
            reportIssue(methodInvocationTree, MESSAGE);
          }
        });
    }
  }
}
