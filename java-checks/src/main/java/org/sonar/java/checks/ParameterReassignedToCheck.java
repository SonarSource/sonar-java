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
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.LiveVariables;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Rule(key = "S1226")
public class ParameterReassignedToCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Set<Symbol> variables = new HashSet<>();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    variables.clear();
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    BlockTree block = tree.block();
    if(block == null) {
      return;
    }
    CFG cfg = CFG.build(tree);
    LiveVariables analyze = LiveVariables.analyze(cfg);
    Set<Symbol> live = analyze.getIn(cfg.entryBlock());
    for (VariableTree parameterTree : tree.parameters()) {
      if(!live.contains(parameterTree.symbol())) {
        variables.add(parameterTree.symbol());
      }
    }
    super.visitMethod(tree);
    for (VariableTree parameterTree : tree.parameters()) {
      if(!live.contains(parameterTree.symbol())) {
        variables.remove(parameterTree.symbol());
      }
    }
  }

  @Override
  public void visitCatch(CatchTree tree) {
    CFG cfg = CFG.buildCFG(tree.block().body(), true);
    Symbol var = tree.parameter().symbol();
    boolean liveVar = true;
    if(var.owner().isMethodSymbol()) {
      cfg.setMethodSymbol((Symbol.MethodSymbol) var.owner());
      LiveVariables analyze = LiveVariables.analyze(cfg);
      Set<Symbol> live = analyze.getIn(cfg.entryBlock());
      liveVar = live.contains(var);
    }
    if(!liveVar) {
      variables.add(var);
    }
    super.visitCatch(tree);
    if(!liveVar) {
      variables.remove(var);
    }
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    CFG cfg = CFG.buildCFG(Collections.singletonList(tree), true);
    Symbol var = tree.variable().symbol();
    boolean liveVar = true;
    if(var.owner().isMethodSymbol()) {
      cfg.setMethodSymbol((Symbol.MethodSymbol) var.owner());
      LiveVariables analyze = LiveVariables.analyze(cfg);
      Set<Symbol> live = analyze.getOut(cfg.reversedBlocks().get(1));
      liveVar = live.contains(var);
    }
    if(!liveVar) {
      variables.add(var);
    }
    super.visitForEachStatement(tree);
    if(!liveVar) {
      variables.remove(var);
    }
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) variable;
      Symbol reference = identifier.symbol();
      if (reference.isVariableSymbol() && variables.contains(reference)) {
        context.reportIssue(this, identifier, "Introduce a new variable instead of reusing the parameter \"" + identifier.name() + "\".");
      }
    }
  }

}
