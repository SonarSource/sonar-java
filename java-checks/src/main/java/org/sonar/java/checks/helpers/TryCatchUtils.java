/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public final class TryCatchUtils {
  private TryCatchUtils() {
    /* This utility class should not be instantiated */
  }

  public static List<Type> getCaughtTypes(CatchTree tree) {
    VariableTree parameter = tree.parameter();
    if (parameter.type() instanceof UnionTypeTree unionTypeTree) {
      return unionTypeTree.typeAlternatives().stream()
        .map(TypeTree::symbolType)
        .toList();
    }
    return List.of(parameter.symbol().type());
  }
}
