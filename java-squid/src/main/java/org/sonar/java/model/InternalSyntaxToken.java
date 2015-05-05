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
package org.sonar.java.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;
import java.util.List;

public class InternalSyntaxToken extends JavaTree implements SyntaxToken {

  private final Token token;
  private List<SyntaxTrivia> trivias;

  public InternalSyntaxToken(AstNodeType astNodeType, Token token, int fromIndex, int toIndex) {
    // Must pass token to super's constructor
    super(astNodeType, token);
    this.token = token;
    this.trivias = createTrivias(token);
    setFromIndex(fromIndex);
    setToIndex(toIndex);
  }

  private InternalSyntaxToken(AstNode astNode) {
    super(astNode);
    this.token = astNode.getToken();
    this.trivias = createTrivias(token);
  }

  public InternalSyntaxToken(Token token) {
    super((AstNode)null);
    this.token = token;
    this.trivias = createTrivias(token);
  }

  @Override
  public String text() {
    return token.getValue();
  }

  @Override
  public List<SyntaxTrivia> trivias() {
    return trivias;
  }

  private List<SyntaxTrivia> createTrivias(Token token) {
    List<SyntaxTrivia> result = Lists.newArrayList();
    for (Trivia trivia : token.getTrivia()) {
      result.add(InternalSyntaxTrivia.create(trivia.getToken().getValue(), trivia.getToken().getLine()));
    }
    return result;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    // FIXME do nothing at the moment
  }

  @Override
  public int getLine() {
    return token.getLine();
  }

  @Override
  public int line() {
    return token.getLine();
  }

  @Override
  public Kind getKind() {
    return Kind.TOKEN;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  public boolean isEOF(){
    return token.getType() == GenericTokenType.EOF;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    throw new UnsupportedOperationException();
  }

  public static InternalSyntaxToken create(AstNode astNode) {
    Preconditions.checkArgument(astNode.hasToken(), "has no token");
    Preconditions.checkArgument(astNode.getToken() == astNode.getLastToken(), "has several tokens");
    return new InternalSyntaxToken(astNode.getType(), astNode.getToken(), astNode.getFromIndex(), astNode.getToIndex());
  }

  public static InternalSyntaxToken createLegacy(AstNode astNode) {
    return new InternalSyntaxToken(astNode);
  }

}
