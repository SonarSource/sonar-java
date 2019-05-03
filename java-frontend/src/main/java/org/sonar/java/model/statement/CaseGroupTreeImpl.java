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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.java.ast.parser.BlockStatementListTreeImpl;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.List;
import java.util.Objects;

public class CaseGroupTreeImpl extends JavaTree implements CaseGroupTree {
  private final List<CaseLabelTree> labels;
  private final List<StatementTree> body;

  public CaseGroupTreeImpl(List<CaseLabelTreeImpl> labels, BlockStatementListTreeImpl body) {
    super(JavaLexer.SWITCH_BLOCK_STATEMENT_GROUP);
    this.labels = ImmutableList.<CaseLabelTree>builder().addAll(Objects.requireNonNull(labels)).build();
    this.body = Objects.requireNonNull(body);
  }

  @Override
  public Kind kind() {
    return Kind.CASE_GROUP;
  }

  @Override
  public List<CaseLabelTree> labels() {
    return labels;
  }

  @Override
  public List<StatementTree> body() {
    return body;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCaseGroup(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Iterables.concat(
      labels,
      body);
  }

}
