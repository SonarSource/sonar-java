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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

/**
 * JDK 17 Preview feature (JEP-406), finalized in JDK 21 (JEP-441).
 */
public class GuardedPatternTreeImpl extends AbstractPatternTree implements GuardedPatternTree {

  private final PatternTree pattern;
  private final SyntaxToken whenOperator;
  private final ExpressionTree expression;

  public GuardedPatternTreeImpl(PatternTree pattern, SyntaxToken whenOperator, ExpressionTree expression, @Nullable ITypeBinding typeBinding) {
    super(Tree.Kind.GUARDED_PATTERN, typeBinding);
    this.pattern = pattern;
    this.whenOperator = whenOperator;
    this.expression = expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitGuardedPattern(this);
  }

  @Override
  public PatternTree pattern() {
    return pattern;
  }

  @Override
  public SyntaxToken whenOperator() {
    return whenOperator;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  protected List<Tree> children() {
    return Arrays.asList(pattern, whenOperator, expression);
  }

}
