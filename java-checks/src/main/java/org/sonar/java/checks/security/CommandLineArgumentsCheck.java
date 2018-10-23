/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.java.model.declaration.MethodTreeImpl;
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

  private static final String ARGS4J_ANNOTATION = "org.kohsuke.args4j.Option";
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
        MethodTreeImpl methodTree = (MethodTreeImpl) member;
        if (methodTree.isMainMethod()) {
          checkMainMethodArgsUsage(methodTree);
        } else {
          checkArgs4JAnnotation(methodTree.simpleName(), methodTree.symbol());
          methodTree.parameters().forEach(p -> checkArgs4JAnnotation(p, p.symbol()));
        }
      } else if (member.is(Tree.Kind.VARIABLE)) {
        checkArgs4JAnnotation(member, ((VariableTree) member).symbol());
      }
    }
  }

  private void checkArgs4JAnnotation(Tree tree, Symbol symbol) {
    if (symbol.metadata().isAnnotatedWith(ARGS4J_ANNOTATION)) {
      reportIssue(tree, MESSAGE);
    }
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
