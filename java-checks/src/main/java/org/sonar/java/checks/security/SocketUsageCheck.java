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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4818")
public class SocketUsageCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Make sure that sockets are used safely here.";
  private static final String JAVAX_NET_SOCKET_FACTORY = "javax.net.SocketFactory";
  private static final String INIT = "<init>";
  private static final String OPEN_METHOD = "open";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE, Tree.Kind.CLASS);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      // === java.net ===
      MethodMatchers.create()
        .ofTypes("java.net.Socket", "java.net.ServerSocket", JAVAX_NET_SOCKET_FACTORY)
        .names(INIT)
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes(JAVAX_NET_SOCKET_FACTORY)
        .names("createSocket")
        .withAnyParameters()
        .build(),

      // === java.nio.channels ===
      MethodMatchers.create()
        .ofTypes(
          "java.nio.channels.AsynchronousServerSocketChannel",
          "java.nio.channels.AsynchronousSocketChannel",
          "java.nio.channels.SocketChannel",
          "java.nio.channels.ServerSocketChannel")
        .names(OPEN_METHOD)
        .withAnyParameters()
        .build(),

      // === Netty ===
      MethodMatchers.create()
        .ofTypes("io.netty.channel.ChannelInitializer")
        .names(INIT)
        .withAnyParameters()
        .build());
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Tree.Kind.CLASS)) {
      checkExtensions(((ClassTree) tree));
    } else {
      super.visitNode(tree);
    }
  }

  private void checkExtensions(ClassTree tree) {
    if (tree.symbol().type().isSubtypeOf(JAVAX_NET_SOCKET_FACTORY) || (tree.symbol().type().isSubtypeOf("io.netty.channel.ChannelInitializer"))) {
      TypeTree superClass = tree.superClass();
      if (superClass != null) {
        // Anonymous class creation will raise issue in `onConstructorFound` method
        reportIssue(superClass, MESSAGE);
      }
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit, MESSAGE);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree, MESSAGE);
  }

}
