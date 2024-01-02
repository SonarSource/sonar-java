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
package org.sonar.java.model.expression;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VarTypeTree;

public class VarTypeTreeImpl extends AbstractTypedTree implements VarTypeTree, JavaTree.AnnotatedTypeTree {

  private final InternalSyntaxToken varToken;

  private List<AnnotationTree> annotations;

  public VarTypeTreeImpl(InternalSyntaxToken varToken) {
    this.varToken = varToken;
    this.annotations =  Collections.emptyList();
  }

  @Override
  public SyntaxToken varToken() {
    return varToken;
  }

  @Override
  public Tree.Kind kind() {
    return Tree.Kind.VAR_TYPE;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitVarType(this);
  }

  @Override
  protected List<Tree> children() {
    return Collections.singletonList(varToken);
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

  @Override
  public void complete(List<AnnotationTree> annotations) {
    this.annotations =  Objects.requireNonNull(annotations);
  }

}
