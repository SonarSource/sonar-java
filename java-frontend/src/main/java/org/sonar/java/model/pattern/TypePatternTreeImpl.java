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
package org.sonar.java.model.pattern;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * JDK 17 Preview feature (JEP-406), finalized in JDK 21 (JEP-441).
 */
public class TypePatternTreeImpl extends AbstractPatternTree implements TypePatternTree {

  private final VariableTree patternVariable;

  public TypePatternTreeImpl(VariableTree patternVariable, @Nullable ITypeBinding typeBinding) {
    super(Tree.Kind.TYPE_PATTERN, typeBinding);
    this.patternVariable = patternVariable;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypePattern(this);
  }

  @Override
  public VariableTree patternVariable() {
    return patternVariable;
  }

  @Override
  protected List<Tree> children() {
    return Collections.singletonList(patternVariable);
  }

}
