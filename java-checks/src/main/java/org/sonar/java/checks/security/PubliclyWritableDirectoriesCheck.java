/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ExpressionsHelper.getInvokedSymbol;

@Rule(key = "S5443")
public class PubliclyWritableDirectoriesCheck extends IssuableSubscriptionVisitor {

  private static final String STRING_TYPE = "java.lang.String";
  private static final String JAVA_NIO_FILE_FILES = "java.nio.file.Files";
  private static final String JAVA_NIO_FILE_PATHS = "java.nio.file.Paths";
  private static final String JAVA_NIO_FILE_PATH = "java.nio.file.Path";
  private static final String JAVA_IO_FILE = "java.io.File";

  private static final String MESSAGE = "Make sure publicly writable directories are used safely here.";
  
  private static final List<String> PUBLIC_WRITABLE_DIRS = Arrays.asList(
    "/tmp",
    "/var/tmp",
    "/usr/tmp",
    "/dev/shm",
    "/dev/mqueue",
    "/run/lock",
    "/var/run/lock",
    "/Library/Caches",
    "/Users/Shared",
    "/private/tmp",
    "/private/var/tmp",
    "\\\\Windows\\\\Temp",
    "\\\\Temp",
    "\\\\TMP");

  private static final Set<String> TMP_DIR_ENV = SetUtils.immutableSetOf("TMP", "TMPDIR");

  private static final MethodMatchers CREATE_FILE_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_NIO_FILE_PATHS, JAVA_NIO_FILE_PATH)
      .names("get")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_NIO_FILE_PATH)
      .names("of")
      .withAnyParameters()
      .build());

  private static final MethodMatchers CREATE_FILE_CONSTRUCTOR_MATCHERS = MethodMatchers.create()
    .ofTypes(JAVA_IO_FILE, "java.io.FileReader")
    .constructor()
    .addParametersMatcher(STRING_TYPE)
    .addParametersMatcher(STRING_TYPE, STRING_TYPE)
    .addParametersMatcher(STRING_TYPE, "java.nio.charset.Charset")
    .build();

  private static final MethodMatchers TEMP_DIR_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_IO_FILE)
    .names("createTempFile")
    .addParametersMatcher(STRING_TYPE, STRING_TYPE)
    .build();

  private static final MethodMatchers NIO_TEMP_DIR_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_NIO_FILE_FILES)
    .names("createTempDirectory")
    .withAnyParameters()
    .build();

  private static final MethodMatchers NIO_TEMP_FILE_MATCHER = MethodMatchers.create()
    .ofTypes(JAVA_NIO_FILE_FILES)
    .names("createTempFile")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MAP_GET = MethodMatchers.create()
    .ofSubTypes("java.util.Map")
    .names("get")
    .addParametersMatcher("java.lang.Object")
    .build();

  private static final MethodMatchers SYSTEM_GETENV = MethodMatchers.create()
    .ofSubTypes("java.lang.System")
    .names("getenv")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (createdInTempDir(mit) || hasSensitiveFileName(mit) || usesSystemTempDir(mit)) {
        reportIssue(tree, MESSAGE);
      }
    } else {
      NewClassTree newClassTree = (NewClassTree) tree;
      if (CREATE_FILE_CONSTRUCTOR_MATCHERS.matches(newClassTree) &&
        isSensitiveFileName(newClassTree.arguments().get(0))) {
        reportIssue(tree, MESSAGE);
      }
    }
  }

  private static boolean hasSensitiveFileName(MethodInvocationTree mit) {
    return CREATE_FILE_MATCHERS.matches(mit) &&
      isSensitiveFileName(mit.arguments().get(0));
  }

  private static boolean usesSystemTempDir(MethodInvocationTree mit) {
    return MAP_GET.matches(mit) && hasTMPAsArgument(mit) && isInitializedWithSystemGetEnv(mit);
  }

  private static boolean hasTMPAsArgument(MethodInvocationTree mit) {
    return mit.arguments().get(0).asConstant(String.class)
      .map(TMP_DIR_ENV::contains)
      .orElse(false);
  }

  private static boolean createdInTempDir(MethodInvocationTree mit) {
    return TEMP_DIR_MATCHER.matches(mit) ||
      (NIO_TEMP_DIR_MATCHER.matches(mit) && (mit.arguments().size() == 1)) ||
      (NIO_TEMP_FILE_MATCHER.matches(mit) && (mit.arguments().size() == 2));
  }

  private static boolean isSensitiveFileName(ExpressionTree expressionTree) {
    return expressionTree.asConstant(String.class)
      .filter(path -> PUBLIC_WRITABLE_DIRS.stream().anyMatch(path::startsWith))
      .isPresent();
  }

  private static boolean isInitializedWithSystemGetEnv(MethodInvocationTree mit) {
    return getInvokedSymbol(mit)
      .filter(ExpressionsHelper::isNotReassigned)
      .map(Symbol::declaration)
      .filter(decl -> decl.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .map(VariableTree::initializer)
      .map(ExpressionUtils::skipParentheses)
      .filter(initializer -> initializer.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .map(SYSTEM_GETENV::matches)
      .orElse(false);
  }

}
