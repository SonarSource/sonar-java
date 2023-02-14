/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2612")
public class FilePermissionsCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String ISSUE_MESSAGE = "Make sure this permission is safe.";
  private static final Set<String> POSIX_OTHER_PERMISSIONS = new HashSet<>(Arrays.asList("OTHERS_READ", "OTHERS_WRITE", "OTHERS_EXECUTE"));
  private static final MethodMatchers POSIX_FILE_PERMISSIONS_FROM_STRING = MethodMatchers.create()
    .ofTypes("java.nio.file.attribute.PosixFilePermissions")
    .names("fromString")
    .addParametersMatcher(JAVA_LANG_STRING)
    .build();

  private static final MethodMatchers RUNTIME_EXEC = MethodMatchers.create()
    .ofTypes("java.lang.Runtime")
    .names("exec")
    .withAnyParameters()
    .build();

  // 'other' group not being 0
  private static final Pattern CHMOD_OCTAL_PATTERN = Pattern.compile("(^|\\s)[0-7]{2,3}[1-7](\\s|$)");

  private static final String WHO = "([ug]*+[ao][ugao]*+)?";
  // ignoring '-' as it should be safe to explicitly REMOVE permission
  private static final String WHAT = "[+=]";
  private static final String WHICH = "[sStT]*+[rwxX][rwxXsStT]*+";
  // simplification of all the possible combinations of adding perms to 'other'
  private static final Pattern SIMPLIFIED_CHMOD_OTHER_PATTERN = Pattern.compile("(^|\\s|,)" + WHO + WHAT + WHICH + "(\\s|,|$)");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IDENTIFIER, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) tree);
    } else {
      checkMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private void checkIdentifier(IdentifierTree identifier) {
    if (isPosixPermission(identifier) && isBeingAdded(identifier)) {
      reportIssue(identifier, ISSUE_MESSAGE);
    }
  }

  private static boolean isPosixPermission(IdentifierTree identifier) {
    return POSIX_OTHER_PERMISSIONS.contains(identifier.name())
      && identifier.symbolType().isSubtypeOf("java.nio.file.attribute.PosixFilePermission");
  }

  private static boolean isBeingAdded(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    while (parent != null) {
      // Whatever the owner of "add" (or "addAll") we assume the property is added to be included
      // ("add" and "addAll" are implemented by all classes extending "java.util.Collection")
      if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
        String methodName = ((MethodInvocationTree) parent).methodSymbol().name();
        if (methodName.contains("add")) {
          return true;
        }
      }
      parent = parent.parent();
    }
    return false;
  }

  private void checkMethodInvocation(MethodInvocationTree mit) {
    if (POSIX_FILE_PERMISSIONS_FROM_STRING.matches(mit)) {
      ExpressionTree arg0 = mit.arguments().get(0);
      if (sensitivePermissionsAsString(arg0)) {
        reportIssue(arg0, ISSUE_MESSAGE);
      }
    } else if (RUNTIME_EXEC.matches(mit)) {
      ExpressionTree arg0 = mit.arguments().get(0);
      Type arg0Type = arg0.symbolType();
      if (arg0Type.is(JAVA_LANG_STRING)) {
        checkExecSingleStringArgument(arg0);
      } else if (arg0Type.is(JAVA_LANG_STRING + "[]") && arg0.is(Tree.Kind.NEW_ARRAY)) {
        // only consider explicit array declaration
        checkExecStringArrayArgument((NewArrayTree) arg0);
      }
    }
  }

  private static boolean sensitivePermissionsAsString(ExpressionTree arg0) {
    return arg0.asConstant(String.class)
      .filter(chmod -> chmod.length() == 9)
      .filter(chmod -> !chmod.endsWith("---"))
      .isPresent();
  }

  private void checkExecSingleStringArgument(ExpressionTree arg0) {
    if (chmodCommand(arg0).filter(FilePermissionsCheck::isSensitiveChmodMode).isPresent()) {
      reportIssue(arg0, ISSUE_MESSAGE);
    }
  }

  private void checkExecStringArrayArgument(NewArrayTree newArrayTree) {
    List<ExpressionTree> initializers = newArrayTree.initializers();
    if (initializers.size() < 3 || !chmodCommand(initializers.get(0)).isPresent()) {
      // malformed or not chmod
      return;
    }
    // check all other arguments against sensitive configuration
    for (int i = 1; i < initializers.size(); i++) {
      ExpressionTree arg = initializers.get(i);
      if (arg.asConstant(String.class).filter(FilePermissionsCheck::isSensitiveChmodMode).isPresent()) {
        reportIssue(arg, ISSUE_MESSAGE);
      }
    }
  }

  private static Optional<String> chmodCommand(ExpressionTree expr) {
    return expr.asConstant(String.class).filter(cmd -> cmd.contains("chmod"));
  }

  private static boolean isSensitiveChmodMode(String mode) {
    return CHMOD_OCTAL_PATTERN.matcher(mode).find() || SIMPLIFIED_CHMOD_OTHER_PATTERN.matcher(mode).find();
  }
}
