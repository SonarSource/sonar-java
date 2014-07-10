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

import com.sun.org.apache.xpath.internal.operations.Variable;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

@Rule(
  key = EnumAsIdentifierCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class EnumAsIdentifierCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1190";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

//  @Override
//  public void init() {
//    subscribeTo(JavaTokenType.IDENTIFIER);
//  }
//
//  @Override
//  public void visitNode(AstNode node) {
//    if ("enum".equals(node.getTokenOriginalValue())) {
//      getContext().createLineViolation(this, "Use a different name than \"enum\".", node);
//    }
//  }

  public void visitClass(ClassTree tree) {
    for (Tree member : tree.members()) {
      checkMember(member);
    }
    super.visitClass(tree);
  }

  public void visitMethod(MethodTree tree) {
    for (VariableTree var: tree.parameters()) {
      checkMember(var);
    }

    if (tree.block() != null && tree.block().body() != null && !tree.block().body().isEmpty()) {
      for (StatementTree stmt : tree.block().body()) {
        checkMember(stmt);
      }
    }
    
    super.visitMethod(tree);
  }

  private void checkMember(Tree candidate) {
    if (candidate.is(Tree.Kind.VARIABLE) ) {
      VariableTree var = (VariableTree) candidate;
      if (var.simpleName().name().equals("enum")) {
        context.addIssue(candidate, ruleKey, "Use a different name than \"enum\".");
      }
    }
  }
}
