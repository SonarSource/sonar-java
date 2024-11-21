/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model.pattern;

import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

/**
 * JDK 19 Preview feature (JEP-405), finalized in JDK 21 (JEP-440).
 */
public class RecordPatternTreeImpl extends AbstractPatternTree implements RecordPatternTree {

  private final TypeTree type;
  private final InternalSyntaxToken openParenToken;
  private final List<PatternTree> patterns;
  private final InternalSyntaxToken closeParenToken;

  public RecordPatternTreeImpl(TypeTree type, InternalSyntaxToken openParenToken, List<PatternTree> patterns, InternalSyntaxToken closeParenToken) {
    super(Kind.RECORD_PATTERN, null);
    this.type = type;
    this.openParenToken = openParenToken;
    this.patterns = patterns;
    this.closeParenToken = closeParenToken;
  }

  @Override
  public Type symbolType() {
    return type.symbolType();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitRecordPattern(this);
  }

  @Override
  public Kind kind() {
    return Kind.RECORD_PATTERN;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public SyntaxToken openParenToken(){
    return openParenToken;
  }

  @Override
  public List<PatternTree> patterns() {
    return patterns;
  }

  @Override
  public SyntaxToken closeParenToken(){
    return closeParenToken;
  }

  @Override
  protected List<Tree> children() {
    return ListUtils.concat(
      List.of(type, openParenToken),
      patterns,
      List.of(closeParenToken)
    );
  }
}
