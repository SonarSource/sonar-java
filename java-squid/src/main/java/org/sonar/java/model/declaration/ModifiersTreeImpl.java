/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.declaration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;
import java.util.List;

public class ModifiersTreeImpl extends JavaTree implements ModifiersTree {
  // TODO remove:
  public static final org.sonar.java.model.declaration.ModifiersTreeImpl EMPTY =
      new org.sonar.java.model.declaration.ModifiersTreeImpl(null, ImmutableList.<Modifier>of(), ImmutableList.<AnnotationTree>of());

  private final List<Modifier> modifiers;
  private final List<AnnotationTree> annotations;

  public ModifiersTreeImpl(AstNode astNode, List<Modifier> modifiers, List<AnnotationTree> annotations) {
    super(astNode);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.annotations = Preconditions.checkNotNull(annotations);
  }

  @Override
  public Kind getKind() {
    return Kind.MODIFIERS;
  }

  @Override
  public List<Modifier> modifiers() {
    return modifiers;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitModifier(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      // TODO(Godin): modifiers
      Iterators.<Tree>emptyIterator(),
      annotations.iterator()
    );
  }
}
