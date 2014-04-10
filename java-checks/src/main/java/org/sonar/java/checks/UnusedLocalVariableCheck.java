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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(
  key = UnusedLocalVariableCheck.RULE_KEY,
  priority = Priority.MAJOR,
  tags={"unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class UnusedLocalVariableCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1481";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        // skip check of field
        super.visitVariable((VariableTree) member);
      } else {
        scan(member);
      }
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.returnType());
    for (VariableTree parameter : tree.parameters()) {
      super.visitVariable(parameter);
    }
    scan(tree.defaultValue());
    scan(tree.block());
  }

  @Override
  public void visitCatch(CatchTree tree) {
    super.visitVariable(tree.parameter());
    scan(tree.block());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    SemanticModel semanticModel = (SemanticModel) context.getSemanticModel();
    Symbol symbol = semanticModel.getSymbol(tree);

    if (symbol != null && semanticModel.getUsages(symbol).isEmpty()) {
      context.addIssue(tree, ruleKey, "Remove this unused \"" + tree.simpleName() + "\" local variable.");
    }
  }

}
