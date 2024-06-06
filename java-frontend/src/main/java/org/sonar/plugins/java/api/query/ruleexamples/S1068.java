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
package org.sonar.plugins.java.api.query.ruleexamples;

import java.util.Map;
import java.util.Set;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.query.CommonTreeQuery;
import org.sonar.plugins.java.api.query.CompilationUnitQuery;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class S1068 extends IssuableJavaCheck {

  public CommonTreeQuery<Map<Symbol, VariableTree>, ? extends Tree> genQuery() {
    return new CompilationUnitQuery<Map<Symbol, VariableTree>>()
      .sequence(
        new CompilationUnitQuery<Map<Symbol, VariableTree>>()
          .subtrees()
          .filterClassTree()
          .members()
          .filterVariableTree()
          .filter((ctx, node) ->
            ModifiersUtils.hasModifier(node.modifiers(), Modifier.PRIVATE) && node.modifiers().annotations().isEmpty()
          ).visit((ctx, node) -> ctx.put(node.symbol(), node)),
        new CompilationUnitQuery<Map<Symbol, VariableTree>>()
          .subtrees()
          .filterIdentifierTree()
          .filter((ctx, node) -> node.symbol().isVariableSymbol() && ctx.containsKey(node.symbol()))
          .visit((ctx, node) -> ctx.remove(node.symbol()))
        ).endVisit((ctx, map) ->
          ctx.values().forEach(fieldDecl ->
            report(fieldDecl,"Remove this unused \""+fieldDecl.simpleName().name()+"\" private field.")
          )
        );
  }
}
