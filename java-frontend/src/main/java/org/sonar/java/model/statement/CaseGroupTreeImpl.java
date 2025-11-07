/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.statement;

import java.util.Collections;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonarsource.analyzer.commons.collections.ListUtils;
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

  public CaseGroupTreeImpl(List<CaseLabelTreeImpl> labels, StatementListTreeImpl body) {
    this.labels = Collections.unmodifiableList(Objects.requireNonNull(labels));
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
  public List<Tree> children() {
    return ListUtils.concat(
      labels,
      body);
  }

}
