/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ComplexityVisitor extends SubscriptionVisitor {

  private List<Tree> blame = new ArrayList<>();
  private Deque<ClassTree> classTrees = new LinkedList<>();
  private boolean analyseAccessors;

  public ComplexityVisitor(boolean analyseAccessors) {
    this.analyseAccessors = analyseAccessors;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.<Tree.Kind>builder()
        .add(Tree.Kind.METHOD)
        .add(Tree.Kind.CONSTRUCTOR)
        .add(Tree.Kind.IF_STATEMENT)
        .add(Tree.Kind.FOR_STATEMENT)
        .add(Tree.Kind.FOR_EACH_STATEMENT)
        .add(Tree.Kind.DO_STATEMENT)
        .add(Tree.Kind.WHILE_STATEMENT)
        .add(Tree.Kind.RETURN_STATEMENT)
        .add(Tree.Kind.THROW_STATEMENT)
        .add(Tree.Kind.CASE_LABEL)
        .add(Tree.Kind.CATCH)
        .add(Tree.Kind.CONDITIONAL_EXPRESSION)
        .add(Tree.Kind.CONDITIONAL_AND)
        .add(Tree.Kind.CONDITIONAL_OR)
        .add(Tree.Kind.CLASS)
        .add(Tree.Kind.ENUM)
        .add(Tree.Kind.ANNOTATION_TYPE)
        .build();
  }

  public List<Tree> scan(ClassTree classTree, MethodTree tree) {
    blame.clear();
    classTrees.clear();
    classTrees.push(classTree);
    super.scanTree(tree);
    return blame;
  }

  public List<Tree> scan(Tree tree) {
    blame.clear();
    classTrees.clear();
    super.scanTree(tree);
    return blame;
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case CLASS:
      case ENUM:
      case ANNOTATION_TYPE:
        classTrees.push((ClassTree) tree);
        break;
      case METHOD:
      case CONSTRUCTOR:
        computeMethodComplexity((MethodTree) tree);
        break;
      case CASE_LABEL:
        CaseLabelTree caseLabelTree = (CaseLabelTree) tree;
        if (!"default".equals(caseLabelTree.caseOrDefaultKeyword().text())) {
          blame.add(caseLabelTree.caseOrDefaultKeyword());
        }
        break;
      case IF_STATEMENT:
      case FOR_STATEMENT:
      case FOR_EACH_STATEMENT:
      case DO_STATEMENT:
      case WHILE_STATEMENT:
      case RETURN_STATEMENT:
      case THROW_STATEMENT:
      case CATCH:
        blame.add(FirstSyntaxTokenFinder.firstSyntaxToken(tree));
        break;
      case CONDITIONAL_EXPRESSION:
        blame.add(((ConditionalExpressionTree) tree).questionToken());
        break;
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        blame.add(((BinaryExpressionTree) tree).operatorToken());
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  private void computeMethodComplexity(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null && (classTrees.isEmpty() || !isAccessor(methodTree))) {
      blame.add(methodTree.simpleName().identifierToken());
    }
  }

  private boolean isAccessor(MethodTree methodTree) {
    return analyseAccessors && AccessorsUtils.isAccessor(classTrees.peek(), methodTree);
  }


  @Override
  public void leaveNode(Tree tree) {
    switch (tree.kind()) {
      case CLASS:
      case ENUM:
      case ANNOTATION_TYPE:
        classTrees.pop();
        break;
      case METHOD:
      case CONSTRUCTOR:
        leaveMethod((MethodTree) tree);
        break;
      default:
        // nothing to do
    }
  }

  private void leaveMethod(MethodTree tree) {
    BlockTree block = tree.block();
    if (block != null && !block.body().isEmpty()) {
      StatementTree last = Iterables.getLast(block.body());
      if (last.is(Tree.Kind.RETURN_STATEMENT)) {
        // minus one because we are going to count the return with +1
        blame.remove(FirstSyntaxTokenFinder.firstSyntaxToken(last));
      }
    }
  }
}
