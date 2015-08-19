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
package org.sonar.java.model.statement;

import com.google.common.collect.Iterators;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

import java.util.Iterator;

public class CaseLabelTreeImpl extends JavaTree implements CaseLabelTree {
  private final InternalSyntaxToken caseOrDefaultKeyword;
  @Nullable
  private final ExpressionTree expression;
  private final InternalSyntaxToken colonToken;

  public CaseLabelTreeImpl(InternalSyntaxToken caseOrDefaultKeyword, @Nullable ExpressionTree expression, InternalSyntaxToken colonToken) {
    super(JavaLexer.SWITCH_LABEL);
    this.caseOrDefaultKeyword = caseOrDefaultKeyword;
    this.expression = expression;
    this.colonToken = colonToken;
  }

  @Override
  public Kind kind() {
    return Kind.CASE_LABEL;
  }

  @Override
  public SyntaxToken caseOrDefaultKeyword() {
    return caseOrDefaultKeyword;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCaseLabel(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>concat(
      Iterators.<Tree>singletonIterator(caseOrDefaultKeyword),
      expression != null ? Iterators.<Tree>singletonIterator(expression) : Iterators.<Tree>emptyIterator(),
      Iterators.<Tree>singletonIterator(colonToken));
  }

}
