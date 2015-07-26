/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class ComplexityVisitor extends SubscriptionVisitor {

  private int complexity;
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

  public int scan(ClassTree classTree, MethodTree tree) {
    complexity = 0;
    classTrees.clear();
    classTrees.push(classTree);
    super.scanTree(tree);
    return complexity;
  }

  public int scan(Tree tree) {
    complexity = 0;
    classTrees.clear();
    super.scanTree(tree);
    return complexity;
  }

  @Override
  public void visitNode(Tree tree) {
    if (isClass(tree)) {
      classTrees.push((ClassTree) tree);
    } else if (isMethod(tree)) {
      computeMethodComplexity((MethodTree) tree);
    } else if (tree.is(Tree.Kind.CASE_LABEL)) {
      CaseLabelTree caseLabelTree = (CaseLabelTree) tree;
      if (!"default".equals(caseLabelTree.caseOrDefaultKeyword().text())) {
        complexity++;
      }
    } else {
      complexity++;
    }
  }

  private static boolean isMethod(Tree tree) {
    return tree.is(Tree.Kind.METHOD) || tree.is(Tree.Kind.CONSTRUCTOR);
  }

  private static boolean isClass(Tree tree) {
    return tree.is(Tree.Kind.CLASS) ||
        tree.is(Tree.Kind.ENUM) ||
        tree.is(Tree.Kind.ANNOTATION_TYPE);
  }

  private void computeMethodComplexity(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null) {
      if (classTrees.isEmpty() || !isAccessor(methodTree)) {
        complexity++;
      }
      if (!block.body().isEmpty() && Iterables.getLast(block.body()).is(Tree.Kind.RETURN_STATEMENT)) {
        //minus one because we are going to count the return with +1
        complexity--;
      }
    }
  }

  private boolean isAccessor(MethodTree methodTree) {
    return analyseAccessors && AccessorsUtils.isAccessor(classTrees.peek(), methodTree);
  }


  @Override
  public void leaveNode(Tree tree) {
    if (isClass(tree)) {
      classTrees.pop();
    }
  }
}
