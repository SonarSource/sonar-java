/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;

/**
 * This class is intended for internal use during semantic analysis and should not be used in checks.
 */
public abstract class AbstractTypedTree extends JavaTree {

  @Nullable
  public ITypeBinding typeBinding;

  public Type symbolType() {
    return typeBinding != null
      ? root.sema.type(typeBinding)
      : Type.UNKNOWN;
  }

}
