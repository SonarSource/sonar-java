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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4823")
public class CommandLineArgumentsCheck extends IssuableSubscriptionVisitor {

  private static final String ARGS4J_OPTION_ANNOTATION = "org.kohsuke.args4j.Option";
  private static final String ARGS4J_ARGUMENT_ANNOTATION = "org.kohsuke.args4j.Argument";
  private static final String MESSAGE = "Make sure that command line arguments are used safely here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ClassTree classTree = (ClassTree) tree;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) member;
        if (MethodTreeUtils.isMainMethod(methodTree)) {
          checkMainMethodArgsUsage(methodTree);
        } else if ("run".equals(methodTree.simpleName().name())) {
          checkArgs4J(methodTree.simpleName(), classTree);
        }
      }
    }
  }

  private void checkArgs4J(IdentifierTree methodIdentifier, ClassTree classTree) {
    List<Tree> args4JAnnotatedMembers = classTree.members().stream()
      .filter(CommandLineArgumentsCheck::hasArgs4JAnnotation)
      .collect(Collectors.toList());

    if (!args4JAnnotatedMembers.isEmpty()) {
      List<JavaFileScannerContext.Location> secondaries = args4JAnnotatedMembers.stream()
        .map(member -> new JavaFileScannerContext.Location("", member))
        .collect(Collectors.toList());
      reportIssue(methodIdentifier, MESSAGE, secondaries, null);
    }
  }

  private static boolean hasArgs4JAnnotation(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      return hasArgs4JAnnotation(((MethodTree) tree).symbol());
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      return hasArgs4JAnnotation(((VariableTree) tree).symbol());
    }
    return false;
  }

  private static boolean hasArgs4JAnnotation(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith(ARGS4J_OPTION_ANNOTATION) || symbol.metadata().isAnnotatedWith(ARGS4J_ARGUMENT_ANNOTATION);
  }

  private void checkMainMethodArgsUsage(MethodTree mainMethod) {
    VariableTree commandLineParameters = mainMethod.parameters().get(0);
    List<IdentifierTree> argsUsages = commandLineParameters.symbol().usages();
    if (!argsUsages.isEmpty()) {
      List<JavaFileScannerContext.Location> secondaries = argsUsages.stream().map(usage -> new JavaFileScannerContext.Location("", usage)).collect(Collectors.toList());
      reportIssue(commandLineParameters, MESSAGE, secondaries, null);
    }
  }

}
