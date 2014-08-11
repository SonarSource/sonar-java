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
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CodeVisitor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class SubscriptionBaseVisitor implements JavaFileScanner, CodeVisitor {


  private JavaFileScannerContext context;
  private Collection<Tree.Kind> nodesToVisit;

  public abstract List<Tree.Kind> nodesToVisit();

  public void visitNode(Tree tree) {
    //Default behavior : do nothing.
  }

  public void leaveNode(Tree tree) {
    //Default behavior : do nothing.
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    nodesToVisit = nodesToVisit();
    visit(context.getTree());
  }

  private void visit(Tree tree) {
    boolean isSubscribed = isSubscribed(tree);
    if(isSubscribed) {
      visitNode(tree);
    }
    visitChildren(tree);
    if(isSubscribed) {
      leaveNode(tree);
    }
  }

  protected boolean isSubscribed(Tree tree) {
    return nodesToVisit.contains(((JavaTree) tree).getKind());
  }

  private void visitChildren(Tree tree) {
    JavaTree javaTree = (JavaTree) tree;
    if (!javaTree.isLeaf()) {
      for (Iterator<Tree> iter = javaTree.childrenIterator(); iter.hasNext(); ) {
        Tree next = iter.next();
        if (next != null) {
          visit(next);
        }
      }
    }
  }

  public void addIssue(Tree tree, String message){
    context.addIssue(tree, RuleKey.of(CheckList.REPOSITORY_KEY, RuleAnnotationUtils.getRuleKey(this.getClass())), message);
  }

}
