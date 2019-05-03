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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.model.AbstractTypedTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class InferedTypeTree extends AbstractTypedTree implements TypeTree{

  public InferedTypeTree(){
    super(null);
  }

  @Override
  public Kind kind() {
    return Kind.INFERED_TYPE;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  @Override
  @Nullable
  public SyntaxToken firstToken() {
    return null;
  }

  @Override
  @Nullable
  public SyntaxToken lastToken() {
    return null;
  }

  @Override
  public Iterable<Tree> children() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    //Do nothing.
  }

  @Override
  public List<AnnotationTree> annotations() {
    return Collections.emptyList();
  }

}
