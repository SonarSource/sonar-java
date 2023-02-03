/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1656")
public class SelfAssignementCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove or correct this useless self-assignment.";
  private final Set<JWarning> warnings = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings.clear();
      warnings.addAll(((JavaTree.CompilationUnitTreeImpl) tree).warnings(JProblem.Type.ASSIGNMENT_HAS_NO_EFFECT));
      return;
    }
    AssignmentExpressionTree node = (AssignmentExpressionTree) tree;
    if (SyntacticEquivalence.areEquivalent(node.expression(), node.variable())) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(reportTree(node))
        .withMessage(ISSUE_MESSAGE)
        .withQuickFix(() -> getQuickFix(node))
        .report();
      updateWarnings(node);
    }
  }

  private static JavaQuickFix getQuickFix(AssignmentExpressionTree tree) {
    ClassTree classParent = (ClassTree) ExpressionUtils.getParentOfType(tree, Tree.Kind.CLASS, Tree.Kind.INTERFACE);
    MethodTree methodParent = (MethodTree) ExpressionUtils.getParentOfType(tree, Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
    String name = getName(tree.variable());

    if (methodParent != null) {
      boolean isParameter = methodParent.parameters().stream()
        .map(p -> p.simpleName().name())
        .anyMatch(p -> p.equals(name));

      if (isParameter) {
        List<String> memberNames = classParent.members().stream()
          .filter(m -> m.is(Tree.Kind.VARIABLE))
          .map(VariableTree.class::cast)
          .map(m -> m.simpleName().name())
          .collect(Collectors.toList());

        if (memberNames.contains(name)) {
          return JavaQuickFix.newQuickFix("Disambiguate this self-assignment")
            .addTextEdit(JavaTextEdit.insertBeforeTree(tree.variable(), "this."))
            .build();
        }
      }
    }
    return JavaQuickFix.newQuickFix("Remove this useless self-assignment")
      .addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(tree, true,
        QuickFixHelper.nextToken(tree), true)))
      .build();
  }

  private static String getName(ExpressionTree variable) {
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) variable).name();
    }
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier().name();
    }
    return "";
  }

  private static SyntaxToken reportTree(AssignmentExpressionTree node) {
    return node.operatorToken();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings.forEach(warning -> {
        AssignmentExpressionTree node = (AssignmentExpressionTree) warning.syntaxTree();
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(reportTree(node))
          .withMessage(ISSUE_MESSAGE)
          .withQuickFix(() -> getQuickFix(node))
          .report();
      });
    }
  }

  private void updateWarnings(AssignmentExpressionTree tree) {
    warnings.removeIf(warning -> tree.equals(warning.syntaxTree()));
  }

}
