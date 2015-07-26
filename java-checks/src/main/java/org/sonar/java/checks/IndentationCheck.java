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
package org.sonar.java.checks;

import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.List;

@Rule(
  key = "IndentationCheck",
  name = "Source code should be indented consistently",
  tags = {"convention"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
public class IndentationCheck extends SubscriptionBaseVisitor {

  private static final Kind[] BLOCK_TYPES = new Kind[] {
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
    description = "Number of white-spaces of an indent. If this property is not set, we just check that the code is indented.",
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
      // Exclude anonymous classes
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
        CaseLabelTree previousCaseLabelTree = labels.get(labels.size() - 2);
        lastCheckedLine = LastSyntaxTokenFinder.lastSyntaxToken(previousCaseLabelTree).line();
      }
    }

    if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      // Exclude anonymous classes
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
      SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(tree);
      if (firstSyntaxToken.column() != expectedLevel && !isExcluded(tree, firstSyntaxToken.line())) {
        addIssue(tree, "Make this line start at column " + (expectedLevel + 1) + ".");
        isBlockAlreadyReported = true;
      }
      lastCheckedLine = LastSyntaxTokenFinder.lastSyntaxToken(tree).line();
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    expectedLevel -= indentationLevel;
    isBlockAlreadyReported = false;
    lastCheckedLine = LastSyntaxTokenFinder.lastSyntaxToken(tree).line();
    if (isClassTree(tree)) {
      isInAnonymousClass.pop();
    }
  }

  private boolean isExcluded(Tree node, int nodeLine) {
    return node.is(Kind.ENUM_CONSTANT) || isBlockAlreadyReported || lastCheckedLine == nodeLine || isInAnonymousClass.peek();
  }

  private static boolean isClassTree(Tree tree) {
    return tree.is(Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.ANNOTATION_TYPE);
  }

}
