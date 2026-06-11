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
package org.sonar.java.utils;

import java.util.Deque;
import java.util.LinkedList;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class PackageUtils {

  private PackageUtils() {
    // Utils class
  }

  /**
   * Returns the package name from a {@link PackageDeclarationTree}, using the given separator
   * between package components (e.g. {@code "."} for Java-style, {@code "/"} for path-style).
   * Returns an empty string when the declaration is {@code null} (default package).
   */
  public static String packageName(@Nullable PackageDeclarationTree packageDeclarationTree, String separator) {
    if (packageDeclarationTree == null) {
      return "";
    }
    Deque<String> pieces = new LinkedList<>();
    ExpressionTree expr = packageDeclarationTree.packageName();
    while (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      pieces.push(mse.identifier().name());
      pieces.push(separator);
      expr = mse.expression();
    }
    pieces.push(((IdentifierTree) expr).name());
    StringBuilder sb = new StringBuilder();
    for (String piece : pieces) {
      sb.append(piece);
    }
    return sb.toString();
  }

  /**
   * Returns the package name of the given symbol by walking up the owner chain
   * until a package symbol is found.
   */
  public static String packageNameOf(Symbol symbol) {
    Symbol owner = symbol.owner();
    while (!owner.isPackageSymbol()) {
      owner = owner.owner();
    }
    return owner.name();
  }
}
