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
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.tree.DefaultPatternTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

/**
 * JDK 17 Preview feature (JEP-406), finalized in JDK 21 (JEP-441).
 */
public class DefaultPatternTreeImpl extends AbstractPatternTree implements DefaultPatternTree {

  private final SyntaxToken defaultToken;

  public DefaultPatternTreeImpl(SyntaxToken defaultToken, @Nullable ITypeBinding typeBinding) {
    super(Tree.Kind.DEFAULT_PATTERN, typeBinding);
    this.defaultToken = defaultToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitDefaultPattern(this);
  }

  @Override
  public SyntaxToken defaultToken() {
    return defaultToken;
  }

  @Override
  protected List<Tree> children() {
    return Collections.singletonList(defaultToken);
  }

}
