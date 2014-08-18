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

import com.google.common.collect.Sets;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Set;

@Rule(
  key = ForLoopCounterChangedCheck.RULE_KEY,
  priority = Priority.MAJOR,
  tags={"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ForLoopCounterChangedCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "ForLoopCounterChangedCheck";
  private final Set<String> loopCounters = Sets.newHashSet();
  private JavaFileScannerContext context;
  private RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    loopCounters.clear();
    scan(context.getTree());
  }


  @Override
  public void visitForStatement(ForStatementTree tree) {
    Set<String> pendingLoopCounters = Sets.newHashSet();
    for (StatementTree statementTree : tree.initializer()) {
      if(statementTree.is(Tree.Kind.VARIABLE)){
        pendingLoopCounters.add(((VariableTree) statementTree).simpleName().name());
      }
    }
    scan(tree.initializer());
    scan(tree.condition());
    scan(tree.update());
    loopCounters.addAll(pendingLoopCounters);
    scan(tree.statement());
    loopCounters.removeAll(pendingLoopCounters);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if(tree.variable().is(Tree.Kind.IDENTIFIER)) {
        checkIdentifier((IdentifierTree) tree.variable());
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if(tree.is( Tree.Kind.PREFIX_INCREMENT) ||
        tree.is(Tree.Kind.POSTFIX_INCREMENT) ||
            tree.is(Tree.Kind.POSTFIX_DECREMENT) ||
                tree.is(Tree.Kind.PREFIX_DECREMENT)) {
      if(tree.expression().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) tree.expression();
        checkIdentifier(identifierTree);
      }
    }
    super.visitUnaryExpression(tree);
  }

  private void checkIdentifier(IdentifierTree identifierTree) {
    if(loopCounters.contains(identifierTree.name())) {
      context.addIssue(identifierTree, ruleKey, "Refactor the code in order to not assign to this loop counter from within the loop body.");
    }
  }

  /*

  @Override
  public void visitFile(AstNode astNode) {
    loopCounters.clear();
  }

  @Override
  public void visitNode(AstNode node) {


    if (node.is(JavaGrammar.FOR_STATEMENT)) {
      pendingLoopCounters = getLoopCounters(node);
    } else if (node.is(JavaGrammar.STATEMENT) && node.getParent().is(JavaGrammar.FOR_STATEMENT)) {
      loopCounters.addAll(pendingLoopCounters);
      pendingLoopCounters = Collections.emptySet();
    } else if (!loopCounters.isEmpty()) {
      if (node.is(JavaGrammar.ASSIGNMENT_EXPRESSION)) {
        for (int i = 0; i < node.getNumberOfChildren() - 1; i++) {
          check(merge(node.getChild(i)), node.getChild(i).getTokenLine());
        }
      } else if (isIncrementOrDecrementExpression(node)) {
        for (AstNode child : node.getChildren()) {
          check(merge(child), child.getTokenLine());
        }
      }
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.FOR_STATEMENT)) {
      loopCounters.removeAll(getLoopCounters(node));
    }
  }

  private static boolean isIncrementOrDecrementExpression(AstNode node) {
    return node.is(JavaGrammar.UNARY_EXPRESSION, JavaGrammar.UNARY_EXPRESSION_NOT_PLUS_MINUS) &&
      node.select()
          .children(JavaGrammar.PREFIX_OP, JavaGrammar.POST_FIX_OP)
          .children(JavaPunctuator.INC, JavaPunctuator.DEC)
          .isNotEmpty();
  }

  private Set<String> getLoopCounters(AstNode node) {
    Set<String> result;

    AstSelect identifiers = node.select()
        .children(JavaGrammar.FOR_INIT)
        .children(JavaGrammar.VARIABLE_DECLARATORS)
        .children(JavaGrammar.VARIABLE_DECLARATOR)
        .children(JavaTokenType.IDENTIFIER);

    if (identifiers.isNotEmpty()) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      for (AstNode identifier : identifiers) {
        builder.add(identifier.getTokenOriginalValue());
      }

      result = builder.build();
    } else {
      result = Collections.emptySet();
    }

    return result;
  }

  private void check(String string, int line) {
    if (loopCounters.contains(string)) {
      getContext().createLineViolation(this, "Refactor the code in order to not assign to this loop counter from within the loop body.", line);
    }
  }

  private static String merge(AstNode node) {
    StringBuilder sb = new StringBuilder();

    for (Token token : node.getTokens()) {
      sb.append(token.getOriginalValue());
    }

    return sb.toString();
  }
*/
}
