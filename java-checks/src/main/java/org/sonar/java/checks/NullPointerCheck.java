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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.AnnotationInstance;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2259",
  name = "Null pointers should not be dereferenced",
  tags = {"bug", "cert", "cwe", "owasp-a1", "owasp-a2", "owasp-a6", "security"},
  priority = Priority.BLOCKER)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("20min")
public class NullPointerCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S2259";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  private JavaFileScannerContext context;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    semanticModel = (SemanticModel) context.getSemanticModel();
    if (semanticModel != null) {
      context.getTree().accept(this);
    }
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    checkForIssue(tree.expression());
    super.visitArrayAccessExpression(tree);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    checkForIssue(tree.expression());
    super.visitMemberSelectExpression(tree);
  }

  // returns true if the symbol is annotated with CheckForNull or Nullable.
  private boolean canBeNull(Symbol symbol) {
    if (symbol != null) {
      for (AnnotationInstance annotation : symbol.metadata().annotations()) {
        if (annotation.isTyped("javax.annotation.CheckForNull") || annotation.isTyped("javax.annotation.Nullable")) {
          return true;
        }
      }
    }
    return false;
  }

  // raises an issue if the passed tree is a method invocation that can return null.
  private void checkForIssue(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTreeImpl) tree).getSymbol();
      if (canBeNull(symbol)) {
        context.addIssue(tree, RULE_KEY, String.format("Value returned by method '%s' can be null.", symbol.getName()));
      }
    }
  }

}
