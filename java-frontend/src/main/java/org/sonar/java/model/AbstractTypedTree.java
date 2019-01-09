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
package org.sonar.java.model;

import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.sslr.grammar.GrammarRuleKey;

/**
 * This class is intended for internal use during semantic analysis and should not be used in checks.
 */
public abstract class AbstractTypedTree extends JavaTree {

  /**
   * Can be {@code null} before and during semantic analysis, but not after.
   */
  // TODO(Godin): never should be null, i.e. better to assign default value
  private Type type;

  public AbstractTypedTree(GrammarRuleKey grammarRuleKey) {
    super(grammarRuleKey);
  }

  /**
   * This method is intended for internal use only during semantic analysis.
   */
  public boolean isTypeSet() {
    return type != null;
  }

  public Type symbolType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setInferedType(Type type) {
    this.type = type;
  }

  public void completeMissingType() {
    if(type == null || ((JavaType) type).isTagged(JavaType.DEFERRED)) {
      type = Symbols.unknownType;
    }
  }

}
