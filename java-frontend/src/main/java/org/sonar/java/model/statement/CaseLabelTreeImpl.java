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
package org.sonar.java.model.statement;

import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class CaseLabelTreeImpl extends JavaTree implements CaseLabelTree {
  private final InternalSyntaxToken caseOrDefaultKeyword;
  private final List<ExpressionTree> expressions;
  private final boolean isFallThrough;
  private final InternalSyntaxToken colonOrArrowToken;

  public CaseLabelTreeImpl(InternalSyntaxToken caseOrDefaultKeyword, List<ExpressionTree> expressions, InternalSyntaxToken colonOrArrowToken) {
    super(JavaLexer.SWITCH_LABEL);
    this.caseOrDefaultKeyword = caseOrDefaultKeyword;
    this.expressions = expressions;
    this.isFallThrough = JavaPunctuator.COLON.getValue().equals(colonOrArrowToken.text());
    this.colonOrArrowToken = colonOrArrowToken;
  }

  @Override
  public Kind kind() {
    return Kind.CASE_LABEL;
  }

  @Override
  public SyntaxToken caseOrDefaultKeyword() {
    return caseOrDefaultKeyword;
  }

  @Override
  public boolean isFallThrough() {
    return isFallThrough;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expressions.isEmpty() ? null : expressions.get(0);
  }

  @Override
  public List<ExpressionTree> expressions() {
    return expressions;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonOrArrowToken;
  }

  @Override
  public SyntaxToken colonOrArrowToken() {
    return colonOrArrowToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCaseLabel(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Iterables.concat(
      Collections.singletonList(caseOrDefaultKeyword),
      expressions,
      Collections.singletonList(colonOrArrowToken));
  }

}
