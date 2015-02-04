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

import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.List;

@Rule(
  key = "IndentationCheck",
  name = "Source code should be correctly indented",
  tags = {"convention"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation(value = "1min")
public class IndentationCheck extends SubscriptionBaseVisitor {

  private static final Kind[] BLOCK_TYPES = new Kind[]{
      Kind.CLASS,
      Kind.INTERFACE,
      Kind.ENUM,
      Kind.ANNOTATION_TYPE,
      Kind.CLASS,
      Kind.BLOCK,
      Kind.STATIC_INITIALIZER,
      Kind.INITIALIZER,
      Kind.SWITCH_STATEMENT,
      Kind.CASE_GROUP
  };

  private static final int DEFAULT_INDENTATION_LEVEL = 2;

  @RuleProperty(
      key = "indentationLevel",
      defaultValue = "" + DEFAULT_INDENTATION_LEVEL)
  public int indentationLevel = DEFAULT_INDENTATION_LEVEL;

  private int expectedLevel;
  private boolean isBlockAlreadyReported;
  private int lastCheckedLine;
  private Deque<Boolean> isInAnonymousClass = Lists.newLinkedList();

  @Override
  public List<Kind> nodesToVisit() {
    return Lists.newArrayList(BLOCK_TYPES);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    expectedLevel = 0;
    isBlockAlreadyReported = false;
    lastCheckedLine = 0;
    isInAnonymousClass.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      //Exclude anonymous classes
      isInAnonymousClass.push(classTree.simpleName() == null);
      if (!isInAnonymousClass.peek()) {
        checkIndentation(Lists.newArrayList(classTree));
      }
    }
    expectedLevel += indentationLevel;
    isBlockAlreadyReported = false;

    if (tree.is(Kind.CASE_GROUP)) {
      List<CaseLabelTree> labels = ((CaseGroupTree) tree).labels();
      if (labels.size() >= 2) {
        lastCheckedLine = ((JavaTree) labels.get(labels.size() - 2)).getAstNode().getLastToken().getLine();
      }
    }

    if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      //Exclude anonymous classes
      if (classTree.simpleName() != null) {
        checkIndentation(classTree.members());
      }
    }
    if (tree.is(Kind.CASE_GROUP)) {
      checkIndentation(((CaseGroupTree) tree).body());
    }
    if (tree.is(Kind.BLOCK)) {
      checkIndentation(((BlockTree) tree).body());
    }
  }

  private void checkIndentation(List<? extends Tree> trees) {
    for (Tree tree : trees) {
      int column = getColumn(tree);
      if (column != expectedLevel && !isExcluded(tree)) {
        addIssue(tree, "Make this line start at column " + (expectedLevel + 1) + ".");
        isBlockAlreadyReported = true;
      }
      lastCheckedLine = ((JavaTree) tree).getLastToken().getLine();
    }
  }

  private int getColumn(Tree tree) {
    if (tree.is(Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) tree;
      int typeColumn = getTypeColumn(variableTree.type());
      if (variableTree.modifiers().isEmpty()) {
        return typeColumn;
      }
      return Math.min(typeColumn, ((JavaTree) variableTree.modifiers()).getToken().getColumn());
    } else if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      if (!classTree.modifiers().isEmpty()) {
        return ((JavaTree) classTree.modifiers()).getToken().getColumn();
      }
    }
    return ((JavaTree) tree).getToken().getColumn();
  }

  private int getTypeColumn(Tree typeTree) {
    if (typeTree.is(Kind.ARRAY_TYPE)) {
      return getTypeColumn(((ArrayTypeTree) typeTree).type());
    }
    return ((JavaTree) typeTree).getToken().getColumn();
  }

  @Override
  public void leaveNode(Tree tree) {
    expectedLevel -= indentationLevel;
    isBlockAlreadyReported = false;
    lastCheckedLine = ((JavaTree) tree).getLastToken().getLine();
    if (isClassTree(tree)) {
      isInAnonymousClass.pop();
    }
  }

  private boolean isExcluded(Tree node) {
    return node.is(Kind.ENUM_CONSTANT) || isBlockAlreadyReported || !isLineFirstStatement((JavaTree) node) || isInAnonymousClass.peek();
  }

  private boolean isLineFirstStatement(JavaTree javaTree) {
    return lastCheckedLine != javaTree.getTokenLine();
  }

  private boolean isClassTree(Tree tree) {
    return tree.is(Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.ANNOTATION_TYPE);
  }

}
