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
package org.sonar.java.checks.serialization;

import java.util.Arrays;
import java.util.List;

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S4926")
public class BlindSerialVersionUidCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.CLASS, Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      Symbol.TypeSymbol symbol = ((ClassTree) tree).symbol();
      if (isSerializable(symbol.type())) {
        findSerialVersionUid(symbol)
          .filter(BlindSerialVersionUidCheck::isValidSerialVersionUid)
          .ifPresent(serialVersionUidSymbol ->
            reportIssue(serialVersionUidSymbol.declaration().simpleName(), "Remove this \"serialVersionUID\".")
          );
      }
    }
  }

  private static Optional<Symbol.VariableSymbol> findSerialVersionUid(Symbol.TypeSymbol symbol) {
    return symbol.lookupSymbols("serialVersionUID").stream()
      .filter(Symbol::isVariableSymbol)
      .map(Symbol.VariableSymbol.class::cast)
      .findFirst();
  }

  private static boolean isValidSerialVersionUid(Symbol.VariableSymbol serialVersionUidSymbol) {
    return serialVersionUidSymbol.isStatic() &&
      serialVersionUidSymbol.isFinal() &&
      serialVersionUidSymbol.type().isPrimitive(Type.Primitives.LONG);
  }

  private static boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }
}
