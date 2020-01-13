/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.sslr.grammar.GrammarRuleKey;

import javax.annotation.Nullable;

/**
 * This class is intended for internal use during semantic analysis and should not be used in checks.
 */
public abstract class AbstractTypedTree extends JavaTree {

  @Nullable
  public ITypeBinding typeBinding;

  public AbstractTypedTree(GrammarRuleKey grammarRuleKey) {
    super(grammarRuleKey);
  }

  public Type symbolType() {
    return typeBinding != null
      ? root.sema.type(typeBinding)
      : Symbols.unknownType;
  }

}
