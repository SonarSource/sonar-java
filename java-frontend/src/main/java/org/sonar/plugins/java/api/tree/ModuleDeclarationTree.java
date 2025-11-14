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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Module declaration.
 *
 * JLS9 - §7.7
 *
 * <pre>
 *   {@link #annotations()} module {@link #moduleName()} { {@link #moduleDirectives() } }
 *   {@link #annotations()} open module {@link #moduleName()} { {@link #moduleDirectives() } }
 * </pre>
 * 
 * @since Java 9
 */
@Beta
public interface ModuleDeclarationTree extends Tree {

  List<AnnotationTree> annotations();

  @Nullable
  SyntaxToken openKeyword();

  SyntaxToken moduleKeyword();

  ModuleNameTree moduleName();

  SyntaxToken openBraceToken();

  List<ModuleDirectiveTree> moduleDirectives();

  SyntaxToken closeBraceToken();
}
