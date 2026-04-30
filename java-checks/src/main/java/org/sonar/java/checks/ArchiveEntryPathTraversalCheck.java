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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S7099")
public class ArchiveEntryPathTraversalCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Validate attacker-controlled path components before using them in file paths or resource lookups.";
  private static final Set<String> PATH_LIKE_PARAMETER_NAMES = Set.of(
    "pathname", "path", "filename", "file", "dirname", "dir", "templatename", "template", "resource", "url");

  private final Set<String> taintedIdentifiers = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD, Tree.Kind.NEW_CLASS, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void setContext(org.sonar.plugins.java.api.JavaFileScannerContext context) {
    super.setContext(context);
    taintedIdentifiers.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      checkMethod((MethodTree) tree);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      checkNewClass((NewClassTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      checkVariable((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      checkAssignment((AssignmentExpressionTree) tree);
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      checkMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private void checkMethod(MethodTree tree) {
    taintedIdentifiers.clear();
    tree.parameters().stream()
      .map(VariableTree::simpleName)
      .filter(id -> id != null && isPathLikeName(id.name()))
      .forEach(id -> taintedIdentifiers.add(id.name()));
  }

  private void checkNewClass(NewClassTree tree) {
    String type = tree.identifier().symbolType().fullyQualifiedName();
    if ("java.io.File".equals(type) && !tree.arguments().isEmpty()) {
      ExpressionTree pathArg = tree.arguments().get(tree.arguments().size() - 1);
      if (isTainted(pathArg)) {
        reportIssue(pathArg, MESSAGE);
      }
      return;
    }
    if (("java.io.FileOutputStream".equals(type) || "java.io.FileInputStream".equals(type)) && !tree.arguments().isEmpty()) {
      ExpressionTree pathArg = tree.arguments().get(0);
      if (isTainted(pathArg)) {
        reportIssue(pathArg, MESSAGE);
      }
      return;
    }
  }

  private void checkVariable(VariableTree tree) {
    ExpressionTree initializer = tree.initializer();
    if (initializer != null && tree.simpleName() != null && isTainted(initializer)) {
      taintedIdentifiers.add(tree.simpleName().name());
    }
  }

  private void checkAssignment(AssignmentExpressionTree tree) {
    if (tree.variable() instanceof IdentifierTree identifier && isTainted(tree.expression())) {
      taintedIdentifiers.add(identifier.name());
    }
  }

  private void checkMethodInvocation(MethodInvocationTree tree) {
    if (!(tree.methodSelect() instanceof MemberSelectExpressionTree mse)) {
      return;
    }
    String methodName = mse.identifier().name();
    if (("mkdir".equals(methodName) || "mkdirs".equals(methodName) || "createNewFile".equals(methodName))
      && isTainted(mse.expression())) {
      reportIssue(mse.identifier(), MESSAGE);
      return;
    }
    if ("getResource".equals(methodName) && !tree.arguments().isEmpty() && isTainted(tree.arguments().get(0))) {
      reportIssue(tree.arguments().get(0), MESSAGE);
    }
  }

  private boolean isTainted(ExpressionTree expr) {
    if (expr == null) {
      return false;
    }
    if (isArchiveEntryGetName(expr) || isRequestGetParameter(expr)) {
      return true;
    }
    if (expr instanceof IdentifierTree identifier) {
      return taintedIdentifiers.contains(identifier.name());
    }
    if (expr instanceof BinaryExpressionTree binary) {
      return isTainted(binary.leftOperand()) || isTainted(binary.rightOperand());
    }
    if (expr instanceof MethodInvocationTree mit) {
      if (isArchiveEntryGetName(mit) || isRequestGetParameter(mit)) {
        return true;
      }
      return mit.arguments().stream().anyMatch(this::isTainted);
    }
    return false;
  }

  private static boolean isArchiveEntryGetName(ExpressionTree expr) {
    if (!(expr instanceof MethodInvocationTree mit)) {
      return false;
    }
    if (!(mit.methodSelect() instanceof MemberSelectExpressionTree mse)) {
      return false;
    }
    if (!"getName".equals(mse.identifier().name())) {
      return false;
    }
    String owner = mse.expression().symbolType().fullyQualifiedName();
    return "java.util.zip.ZipEntry".equals(owner) || "java.util.jar.JarEntry".equals(owner);
  }

  private static boolean isRequestGetParameter(ExpressionTree expr) {
    if (!(expr instanceof MethodInvocationTree mit)) {
      return false;
    }
    if (!(mit.methodSelect() instanceof MemberSelectExpressionTree mse)) {
      return false;
    }
    return "getParameter".equals(mse.identifier().name());
  }

  private static boolean isPathLikeName(String name) {
    name = name.toLowerCase();
    return PATH_LIKE_PARAMETER_NAMES.stream().anyMatch(name::contains);
  }
}
