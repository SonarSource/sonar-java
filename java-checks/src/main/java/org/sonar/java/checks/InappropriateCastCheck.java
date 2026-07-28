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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S1944")
public class InappropriateCastCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeCastTree castTree = (TypeCastTree) tree;
    Type sourceType = castTree.expression().symbolType().erasure();
    Type targetType = castTree.type().symbolType().erasure();

    if (shouldSkip(sourceType) || shouldSkip(targetType)) {
      return;
    }

    if (sourceType.isSubtypeOf(targetType) || targetType.isSubtypeOf(sourceType)) {
      return;
    }

    if (areNeitherInterfaces(sourceType, targetType) || areTypesFinalClassAndInterface(sourceType, targetType)) {
      reportIssue(castTree.type(),
        String.format("\"%s\" cannot be cast to \"%s\" without a risk of \"ClassCastException\".",
          sourceType.name(), targetType.name()));
    }
  }

  private static boolean shouldSkip(Type type) {
    return type.isUnknown()
      || type.isPrimitive()
      || type.isVoid()
      || type.isNullType()
      || type.isTypeVar()
      || type.isArray()
      || type.is("java.lang.Object");
  }

  private static boolean areNeitherInterfaces(Type type1, Type type2) {
    return !type1.symbol().isInterface() && !type2.symbol().isInterface();
  }

  private static boolean areTypesFinalClassAndInterface(Type type1, Type type2) {
    return (type1.symbol().isInterface() && type2.symbol().isFinal())
      || (type2.symbol().isInterface() && type1.symbol().isFinal());
  }

}
