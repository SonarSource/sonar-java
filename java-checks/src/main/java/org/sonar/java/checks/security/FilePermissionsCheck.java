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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2612")
public class FilePermissionsCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String ISSUE_MESSAGE = "Make sure this permission is safe.";
  private static final Set<String> POSIX_OTHER_PERMISSIONS = new HashSet<>(Arrays.asList("OTHERS_READ", "OTHERS_WRITE", "OTHERS_EXECUTE"));
  private static final MethodMatcher POSIX_FILE_PERMISSIONS_FROM_STRING = MethodMatcher.create()
    .name("fromString")
    .typeDefinition("java.nio.file.attribute.PosixFilePermissions")
    .parameters(JAVA_LANG_STRING);

  private static final MethodMatcher RUNTIME_EXEC = MethodMatcher.create()
    .name("exec")
    .typeDefinition("java.lang.Runtime")
    .withAnyParameters();

  // 'other' group not being 0
  private static final Pattern CHMOD_OCTAL_PATTERN = Pattern.compile("[0-7]{2}[1-7]");
  // simplification of all the possible combinations of adding perms to 'other'
  private static final Pattern SIMPLIFIED_CHMOD_OTHER_PATTERN = Pattern.compile("o[+=](r?w?x?)+");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IDENTIFIER, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      check((IdentifierTree) tree);
    } else {
      check((MethodInvocationTree) tree);
    }
  }

  private void check(IdentifierTree identifier) {
    if (isPosixPermission(identifier) && !isBeingRemoved(identifier)) {
      reportIssue(identifier, ISSUE_MESSAGE);
    }
  }

  private static boolean isPosixPermission(IdentifierTree identifier) {
    return POSIX_OTHER_PERMISSIONS.contains(identifier.name())
      && identifier.symbolType().isSubtypeOf("java.nio.file.attribute.PosixFilePermission");
  }

  private static boolean isBeingRemoved(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    while (parent != null) {
      // Whatever the owner of "remove", we assume the property is dropped if calling a method "remove"
      // (implemented by all classes extending Collection)
      if (parent.is(Tree.Kind.METHOD_INVOCATION) && ((MethodInvocationTree) parent).symbol().name().contains("remove")) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private void check(MethodInvocationTree mit) {
    if (POSIX_FILE_PERMISSIONS_FROM_STRING.matches(mit)) {
      ExpressionTree arg0 = mit.arguments().get(0);
      if (sensitivePermissionsAsString(arg0)) {
        reportIssue(arg0, ISSUE_MESSAGE);
      }
    } else if (RUNTIME_EXEC.matches(mit)) {
      ExpressionTree arg0 = mit.arguments().get(0);
      if (arg0.symbolType().is(JAVA_LANG_STRING) && sensitiveChmodCommand(arg0)) {
        reportIssue(arg0, ISSUE_MESSAGE);
      }
    }
  }

  private static boolean sensitivePermissionsAsString(ExpressionTree arg0) {
    return arg0.asConstant(String.class)
      .filter(chmod -> chmod.length() == 9)
      .filter(chmod -> !chmod.endsWith("---"))
      .isPresent();
  }

  private static boolean sensitiveChmodCommand(ExpressionTree arg0) {
    return arg0.asConstant(String.class)
      .filter(cmd -> cmd.trim().startsWith("chmod"))
      .filter(cmd -> CHMOD_OCTAL_PATTERN.matcher(cmd).find() || SIMPLIFIED_CHMOD_OTHER_PATTERN.matcher(cmd).find())
      .isPresent();
  }
}
