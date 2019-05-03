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
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S1659")
public class OneDeclarationPerLineCheck extends IssuableSubscriptionVisitor {

  private boolean varSameDeclaration;
  private int lastVarLine;

  @Override
  public void setContext(JavaFileScannerContext context) {
    lastVarLine = -1;
    varSameDeclaration = false;
    super.setContext(context);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.INTERFACE, Kind.CLASS, Kind.ENUM, Kind.ANNOTATION_TYPE, Kind.BLOCK, Kind.STATIC_INITIALIZER, Kind.CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.INTERFACE, Kind.CLASS, Kind.ENUM, Kind.ANNOTATION_TYPE)) {
      // Field class declaration
      checkVariables(((ClassTree) tree).members());
    } else if (tree.is(Kind.BLOCK, Kind.STATIC_INITIALIZER)) {
      // Local variable declaration (in method, static initialization, ...)
      checkVariables(((BlockTree) tree).body());
    } else if (tree.is(Kind.CASE_GROUP)) {
      checkVariables(((CaseGroupTree) tree).body());
    }
  }

  private void checkVariables(List<? extends Tree> trees) {
    for (Tree tree : trees) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        checkVariable((VariableTree) tree);
      }
    }
  }

  private void checkVariable(VariableTree varTree) {
    int line = varTree.simpleName().identifierToken().line();
    if (varSameDeclaration || lastVarLine == line) {
      reportIssue(varTree.simpleName(), String.format("Declare \"%s\" on a separate line.", varTree.simpleName().name()));
    }
    varSameDeclaration = ",".equals(varTree.endToken().text());
    lastVarLine = line;
  }
}
