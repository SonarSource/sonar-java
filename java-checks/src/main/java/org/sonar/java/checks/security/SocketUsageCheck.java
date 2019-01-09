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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
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
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      // === java.net ===
      MethodMatcher.create().typeDefinition("java.net.Socket").name(INIT).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.net.ServerSocket").name(INIT).withAnyParameters(),
      MethodMatcher.create().typeDefinition(JAVAX_NET_SOCKET_FACTORY).name(INIT).withAnyParameters(),
      MethodMatcher.create().typeDefinition(JAVAX_NET_SOCKET_FACTORY).name("createSocket").withAnyParameters(),

      // === java.nio.channels ===
      MethodMatcher.create().typeDefinition("java.nio.channels.AsynchronousServerSocketChannel").name(OPEN_METHOD).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.nio.channels.AsynchronousSocketChannel").name(OPEN_METHOD).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.nio.channels.SocketChannel").name(OPEN_METHOD).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.nio.channels.ServerSocketChannel").name(OPEN_METHOD).withAnyParameters(),

      // === Netty ===
      MethodMatcher.create().typeDefinition("io.netty.channel.ChannelInitializer").name(INIT).withAnyParameters());
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
