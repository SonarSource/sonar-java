/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

/**
 * JDK 19 Preview feature (JEP-405), finalized in JDK 21 (JEP-440).
 */
public class RecordPatternTreeImpl extends AbstractPatternTree implements RecordPatternTree {

  private final TypeTree type;
  private final List<PatternTree> patterns;

  public RecordPatternTreeImpl(TypeTree type, List<PatternTree> patterns) {
    super(Kind.RECORD_PATTERN);
    this.type = type;
    this.patterns = patterns;
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
  public List<PatternTree> patterns() {
    return patterns;
  }

  @Override
  protected List<Tree> children() {
    return ListUtils.concat(
      Collections.singleton(type),
      patterns
    );
  }
}
