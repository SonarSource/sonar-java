/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model.pattern;

import java.util.Arrays;
import java.util.List;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

/**
 * JDK 17 Preview feature  (JEP-406), deprecated by design until it will be final
 * java:S1874 = "@Deprecated" code should not be used
 */
@SuppressWarnings("java:S1874")
public class GuardedPatternTreeImpl extends AbstractPatternTree implements GuardedPatternTree {

  private final PatternTree pattern;
  private final SyntaxToken andOperator;
  private final ExpressionTree expression;

  public GuardedPatternTreeImpl(PatternTree pattern, SyntaxToken andOperator, ExpressionTree expression) {
    super(Tree.Kind.GUARDED_PATTERN);
    this.pattern = pattern;
    this.andOperator = andOperator;
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
  public SyntaxToken andOperator() {
    return andOperator;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  protected List<Tree> children() {
    return Arrays.asList(pattern, andOperator, expression);
  }

}
