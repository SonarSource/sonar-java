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
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;

/**
 * Method or annotation type element declaration.
 * <p>
 * JLS 8.8. Constructor ({@link Tree.Kind#CONSTRUCTOR}):
 * <pre>
 *   {@link #modifiers()} {@link #typeParameters()} {@link #simpleName()} ( {@link #parameters()} ) throws {@link #throwsClauses()} {@link #block()}
 * </pre>
 * JLS 8.4, 9.4. Method ({@link Tree.Kind#METHOD}):
 * <pre>
 *   {@link #modifiers()} {@link #typeParameters()} {@link #returnType()} {@link #simpleName()} ( {@link #parameters()} ) throws {@link #throwsClauses()} {@link #block()}
 * </pre>
 * JLS 9.6.1, 9.6.2. Annotation type element ({@link Tree.Kind#METHOD}):
 * <pre>
 *   {@link #modifiers()} {@link #returnType()} {@link #simpleName()} default {@link #defaultValue()} ;
 * </pre>
 * </p>
 *
 * @since Java 1.3
 */
@Beta
public interface MethodTree extends Tree {

  ModifiersTree modifiers();

  TypeParameters typeParameters();

  /**
   * @return null in case of constructor
   */
  @Nullable
  TypeTree returnType();

  IdentifierTree simpleName();

  SyntaxToken openParenToken();

  List<VariableTree> parameters();

  SyntaxToken closeParenToken();

  SyntaxToken throwsToken();

  ListTree<TypeTree> throwsClauses();

  @Nullable
  BlockTree block();

  @Nullable
  SyntaxToken semicolonToken();

  /**
   * @since Java 1.5
   */
  @Nullable
  SyntaxToken defaultToken();

  /**
   * @since Java 1.5
   */
  @Nullable
  ExpressionTree defaultValue();

  Symbol.MethodSymbol symbol();

  /**
   * Check if a methodTree is overriding any other method. The corresponding overridden symbol can be retrieved through the {@link #symbol()}.
   *
   * @return true if overriding, null if it cannot be decided (method symbol not resolved or lack of byte code for super types), false if not overriding.
   */
  @Nullable
  Boolean isOverriding();

  /**
   * Compute a CFG for a given method.
   *
   * @return null if the method as no body. Otherwise the corresponding CFG.
   */
  @Nullable
  ControlFlowGraph cfg();
}
