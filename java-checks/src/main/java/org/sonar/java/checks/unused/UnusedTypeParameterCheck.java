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
package org.sonar.java.checks.unused;

import org.sonar.check.Rule;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2326")
public class UnusedTypeParameterCheck extends IssuableSubscriptionVisitor {

  private SemanticModel semanticModel;

  @Override
  public void setContext(JavaFileScannerContext context) {
    semanticModel = (SemanticModel) context.getSemanticModel();
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      TypeParameters typeParameters;
      String messageEnd;
      if (tree.is(Tree.Kind.METHOD)) {
        typeParameters = ((MethodTree) tree).typeParameters();
        messageEnd = "method.";
      } else {
        typeParameters = ((ClassTree) tree).typeParameters();
        messageEnd = "class.";
        if (tree.is(Tree.Kind.INTERFACE)) {
          messageEnd = "interface.";
        }
      }
      for (TypeParameterTree typeParameter : typeParameters) {
        Symbol symbol = semanticModel.getSymbol(typeParameter);
        if (symbol.usages().isEmpty()) {
          String message = new StringBuilder(typeParameter.identifier().name())
            .append(" is not used in the ")
            .append(messageEnd).toString();
          reportIssue(typeParameter.identifier(), message);
        }
      }
    }
  }
}
