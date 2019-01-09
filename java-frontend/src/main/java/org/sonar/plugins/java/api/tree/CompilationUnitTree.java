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

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Compilation unit.
 *
 * JLS 7.3 and 7.4
 *
 * @since Java 1.3
 */
@Beta
public interface CompilationUnitTree extends Tree {

  @Nullable
  PackageDeclarationTree packageDeclaration();

  List<ImportClauseTree> imports();

  List<Tree> types();

  /**
   * Experimental feature allowing retrieval of java 9 module declaration from 'module-info.java' files.
   *
   * In java 9, a new compilation unit level has been introduced, splitting current compilation units between 
   * 'Modular' and 'Ordinary' Compilation Units. In order to not introduce breaking change in API too early, and as long as java 9 
   * is not officially released, the 'Module Declaration' part of the Java 9 'Modular Compilation Unit' will be part of the
   * current Compilation Unit interface.
   *
   * @since Java 9
   */
  @Nullable
  ModuleDeclarationTree moduleDeclaration();

  SyntaxToken eofToken();

}
