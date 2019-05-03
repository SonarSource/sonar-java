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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Rule(key = "S3340")
public class AnnotationArgumentOrderCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotationTree = (AnnotationTree) tree;
    TypeSymbol annotationSymbol = annotationTree.symbolType().symbol();
    if (annotationSymbol.isUnknown()) {
      return;
    }
    List<String> declarationNames = new ArrayList<>();
    for (Symbol symbol : annotationSymbol.memberSymbols()) {
      declarationNames.add(symbol.name());
    }
    List<String> annotationArguments = new ArrayList<>();
    for (ExpressionTree argument : annotationTree.arguments()) {
      if (argument.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignmentTree = (AssignmentExpressionTree) argument;
        IdentifierTree nameTree = (IdentifierTree) assignmentTree.variable();
        annotationArguments.add(nameTree.name());
      }
    }
    declarationNames.retainAll(annotationArguments);
    if (!declarationNames.equals(annotationArguments)) {
      reportIssue(annotationTree.annotationType(), "Reorder annotation arguments to match the order of declaration.");
    }
  }

}
