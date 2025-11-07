/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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

@Rule(key = "S4926")
public class BlindSerialVersionUidCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol.TypeSymbol symbol = ((ClassTree) tree).symbol();
    if (isSerializable(symbol.type())) {
      findSerialVersionUid(symbol)
        .filter(BlindSerialVersionUidCheck::isValidSerialVersionUid)
        .map(serialVersionUidSymbol -> serialVersionUidSymbol.declaration().simpleName())
        .ifPresent(declarationTree -> reportIssue(declarationTree, "Remove this \"serialVersionUID\"."));
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
