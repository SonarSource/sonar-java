/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.ast.visitors;

import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.EnumSet;
import java.util.List;

public abstract class SubscriptionVisitor implements JavaFileScanner {


  protected JavaFileScannerContext context;
  private EnumSet<Tree.Kind> nodesToVisit;
  private boolean visitToken;
  private boolean visitTrivia;
  private SemanticModel semanticModel;

  public abstract List<Tree.Kind> nodesToVisit();

  public void visitNode(Tree tree) {
    //Default behavior : do nothing.
  }

  public void leaveNode(Tree tree) {
    //Default behavior : do nothing.
  }

  public void visitToken(SyntaxToken syntaxToken) {
    //default behaviour is to do nothing
  }

  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    //default behaviour is to do nothing
  }

  public void setContext(JavaFileScannerContext context) {
    this.context = context;
    semanticModel = (SemanticModel) context.getSemanticModel();
  }

  public void leaveFile(JavaFileScannerContext context) {
    //default behaviour is to do nothing
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    setContext(context);
    scanTree(context.getTree());
  }

  protected void scanTree(Tree tree) {
    if(nodesToVisit == null) {
      List<Tree.Kind> kinds = nodesToVisit();
      if(kinds.isEmpty()) {
        nodesToVisit = EnumSet.noneOf(Tree.Kind.class);
      } else {
        nodesToVisit = EnumSet.copyOf(kinds);
      }
    }
    visitToken = isVisitingTokens();
    visitTrivia = isVisitingTrivia();
    visit(tree);
  }

  private void visit(Tree tree) {
    boolean isSubscribed = isSubscribed(tree);
    boolean shouldVisitSyntaxToken = (visitToken || visitTrivia) && tree.is(Tree.Kind.TOKEN);
    if (shouldVisitSyntaxToken) {
      SyntaxToken syntaxToken = (SyntaxToken) tree;
      if (visitToken) {
        visitToken(syntaxToken);
      }
      if (visitTrivia) {
        for (SyntaxTrivia syntaxTrivia : syntaxToken.trivias()) {
          visitTrivia(syntaxTrivia);
        }
      }
    } else if (isSubscribed) {
      visitNode(tree);
    }
    visitChildren(tree);
    if (!shouldVisitSyntaxToken && isSubscribed) {
      leaveNode(tree);
    }
  }

  private boolean isSubscribed(Tree tree) {
    return nodesToVisit.contains(tree.kind());
  }

  private boolean isVisitingTrivia() {
    return nodesToVisit.contains(Tree.Kind.TRIVIA);
  }

  private boolean isVisitingTokens() {
    return nodesToVisit.contains(Tree.Kind.TOKEN);
  }

  private void visitChildren(Tree tree) {
    JavaTree javaTree = (JavaTree) tree;
    if (!javaTree.isLeaf()) {
      for (Tree next : javaTree.getChildren()) {
        if (next != null) {
          visit(next);
        }
      }
    }
  }

  public boolean hasSemantic(){
    return semanticModel != null;
  }
}
