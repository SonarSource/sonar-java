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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.Token;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class SubscriptionVisitor implements JavaFileScanner {


  protected JavaFileScannerContext context;
  private Collection<Tree.Kind> nodesToVisit;
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

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    semanticModel = (SemanticModel) context.getSemanticModel();
    scanTree(context.getTree());
    visitTokens(context.getTree());
  }

  protected void scanTree(Tree tree) {
    nodesToVisit = nodesToVisit();
    visit(tree);
  }

  protected void visitTokens(CompilationUnitTree compilationUnitTree) {
    if (nodesToVisit().contains(Tree.Kind.TOKEN) || nodesToVisit().contains(Tree.Kind.TRIVIA)) {
      //FIXME relying on ASTNode to iterate over tokens.
      for (Token token : ((JavaTree) compilationUnitTree).getAstNode().getTokens()) {
        SyntaxToken syntaxToken = new InternalSyntaxToken(token);
        visitToken(syntaxToken);
        if (nodesToVisit().contains(Tree.Kind.TRIVIA)) {
          for (SyntaxTrivia syntaxTrivia : syntaxToken.trivias()) {
            visitTrivia(syntaxTrivia);
          }
        }
      }
    }
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

  public boolean hasSemantic(){
    return semanticModel != null;
  }
}
