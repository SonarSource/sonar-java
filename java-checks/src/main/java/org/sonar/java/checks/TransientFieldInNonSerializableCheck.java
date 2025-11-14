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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2065")
public class TransientFieldInNonSerializableCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isNotSerializable(classTree.symbol())) {
      for (Tree member : classTree.members()) {
        ModifierKeywordTree transientModifier = isTransient(member);
        if (transientModifier != null) {
          reportIssue(transientModifier, "Remove the \"transient\" modifier from this field.");
        }
      }
    }
  }

  private static boolean isNotSerializable(Symbol.TypeSymbol symbol) {
    for (Type superType : symbol.superTypes()) {
      if (superType.isUnknown()) {
        return false;
      }
    }
    return !symbol.type().isSubtypeOf("java.io.Serializable");
  }

  private static ModifierKeywordTree isTransient(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      return ModifiersUtils.getModifier(variable.modifiers(), Modifier.TRANSIENT);
    }
    return null;
  }

}
