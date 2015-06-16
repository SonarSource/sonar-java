/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1659",
  name = "Multiple variables should not be declared on the same line",
  tags = {"convention"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class OneDeclarationPerLineCheck extends SubscriptionBaseVisitor {

  private boolean varSameDeclaration;
  private int lastVarLine;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    // Check is singleton => line storage clean (corner case : previous file last variable line = next file first variable line)
    lastVarLine = -1;
    super.scanFile(context);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.BLOCK, Tree.Kind.STATIC_INITIALIZER);
  }

  @Override
  public void visitNode(Tree tree) {
    // Field class declaration
    if (tree instanceof ClassTree) {
      for (Tree member : ((ClassTree) tree).members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          checkVariable((VariableTree) member);
        }
      }
    }
    // Local variable declaration (in method, static initialization, ...)
    if (tree instanceof BlockTree) {
      for (StatementTree statment : ((BlockTree) tree).body()) {
        if (statment.is(Tree.Kind.VARIABLE)) {
          checkVariable((VariableTree) statment);
        }
      }
    }
  }

  private void checkVariable(VariableTree varTree) {
    if (varSameDeclaration || lastVarLine == varTree.simpleName().identifierToken().line()) {
      addIssue(varTree, String.format("Declare \"%s\" on a separate line.", varTree.symbol().name()));
    }
    varSameDeclaration = ",".equals(varTree.endToken().text());
    lastVarLine = varTree.simpleName().identifierToken().line();
  }
}
