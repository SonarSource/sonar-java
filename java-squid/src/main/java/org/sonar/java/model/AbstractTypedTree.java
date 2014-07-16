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
package org.sonar.java.model;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.resolve.Type;

public abstract class AbstractTypedTree extends JavaTree {
  /**
   * Can be {@code null} before and during semantic analysis, but not after.
   */
  // TODO(Godin): never should be null, i.e. better to assign default value
  private Type type;

  public AbstractTypedTree(AstNode astNode) {
    super(astNode);
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    // FIXME(Godin): type should be computed and set only once, but currently this is not the case and this contract is violated
//    Preconditions.checkState(this.type == null);
    this.type = type;
  }
}
