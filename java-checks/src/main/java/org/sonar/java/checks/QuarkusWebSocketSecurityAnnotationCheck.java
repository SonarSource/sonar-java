/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8914")
public class QuarkusWebSocketSecurityAnnotationCheck extends IssuableSubscriptionVisitor {

  private static final String WEBSOCKET_ANNOTATION = "io.quarkus.websockets.next.WebSocket";
  private static final String AUTHENTICATED_ANNOTATION = "io.quarkus.security.Authenticated";
  private static final String ROLES_ALLOWED_ANNOTATION = "jakarta.annotation.security.RolesAllowed";

  private static final List<String> WEBSOCKET_CALLBACK_ANNOTATIONS = List.of(
    "io.quarkus.websockets.next.OnOpen",
    "io.quarkus.websockets.next.OnTextMessage",
    "io.quarkus.websockets.next.OnBinaryMessage",
    "io.quarkus.websockets.next.OnClose",
    "io.quarkus.websockets.next.OnError",
    "io.quarkus.websockets.next.OnPongMessage"
  );

  private static final List<String> SECURITY_ANNOTATIONS = List.of(
    AUTHENTICATED_ANNOTATION,
    ROLES_ALLOWED_ANNOTATION
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    // Check if the class is annotated with @WebSocket
    boolean isWebSocketEndpoint = classTree.modifiers().annotations().stream()
      .anyMatch(annotation -> annotation.annotationType().symbolType().is(WEBSOCKET_ANNOTATION));

    if (!isWebSocketEndpoint) {
      return;
    }

    // Check if the class has class-level security annotations
    boolean hasClassLevelSecurity = classTree.modifiers().annotations().stream()
      .anyMatch(annotation -> SECURITY_ANNOTATIONS.stream()
        .anyMatch(securityAnnotation -> annotation.annotationType().symbolType().is(securityAnnotation)));

    // If class already has security at class level, no issues to report
    if (hasClassLevelSecurity) {
      return;
    }

    // Check methods for security annotations on WebSocket callbacks
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .forEach(method -> checkMethod(method));
  }

  private void checkMethod(MethodTree methodTree) {
    // Check if the method is a WebSocket callback
    boolean isWebSocketCallback = methodTree.modifiers().annotations().stream()
      .anyMatch(annotation -> WEBSOCKET_CALLBACK_ANNOTATIONS.stream()
        .anyMatch(callbackAnnotation -> annotation.annotationType().symbolType().is(callbackAnnotation)));

    if (!isWebSocketCallback) {
      return;
    }

    // Find security annotations on the method
    List<AnnotationTree> securityAnnotations = methodTree.modifiers().annotations().stream()
      .filter(annotation -> SECURITY_ANNOTATIONS.stream()
        .anyMatch(securityAnnotation -> annotation.annotationType().symbolType().is(securityAnnotation)))
      .toList();

    // Report issue for each security annotation on the method
    for (AnnotationTree securityAnnotation : securityAnnotations) {
      reportIssue(
        securityAnnotation,
        "Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.",
        List.of(),
        null
      );
    }
  }
}
