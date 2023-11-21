/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2057")
public class SerialVersionUidCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    visitClassTree((ClassTree) tree);
  }

  private void visitClassTree(ClassTree classTree) {
    Symbol.TypeSymbol symbol = classTree.symbol();
    IdentifierTree simpleName = classTree.simpleName();
    if (simpleName != null && isSerializable(symbol.type())) {
      Symbol.VariableSymbol serialVersionUidSymbol = findSerialVersionUid(symbol);
      if (serialVersionUidSymbol == null) {
        if (!isExclusion(symbol)) {
          reportIssue(simpleName, "Add a \"static final long serialVersionUID\" field to this class.");
        }
      } else {
        checkModifiers(serialVersionUidSymbol);
      }
    }
  }

  private void checkModifiers(Symbol.VariableSymbol serialVersionUidSymbol) {
    List<String> missingModifiers = new ArrayList<>();
    if (!serialVersionUidSymbol.isStatic()) {
      missingModifiers.add("static");
    }
    if (!serialVersionUidSymbol.isFinal()) {
      missingModifiers.add("final");
    }
    if (!serialVersionUidSymbol.type().is("long")) {
      missingModifiers.add("long");
    }
    VariableTree variableTree = serialVersionUidSymbol.declaration();
    if (variableTree != null && !missingModifiers.isEmpty()) {
      reportIssue(variableTree.simpleName(), "Make this \"serialVersionUID\" field \"" + String.join(" ", missingModifiers) + "\".");
    }
  }

  private static Symbol.VariableSymbol findSerialVersionUid(Symbol.TypeSymbol symbol) {
    for (Symbol member : symbol.lookupSymbols("serialVersionUID")) {
      if (member.isVariableSymbol()) {
        return (Symbol.VariableSymbol) member;
      }
    }
    return null;
  }

  private static boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean isExclusion(Symbol.TypeSymbol symbol) {
    return symbol.isAbstract()
      || symbol.type().isSubtypeOf("java.lang.Throwable")
      || isGuiClass(symbol);
  }

  private static boolean isGuiClass(Symbol.TypeSymbol symbol) {
    for (Type superType : symbol.superTypes()) {
      Symbol.TypeSymbol superTypeSymbol = superType.symbol();
      if (hasGuiPackage(superTypeSymbol)) {
        return true;
      }
    }
    return hasGuiPackage(symbol) || (!symbol.equals(symbol.outermostClass()) && isGuiClass(symbol.outermostClass()));
  }

  private static boolean hasGuiPackage(Symbol.TypeSymbol superTypeSymbol) {
    String fullyQualifiedName = superTypeSymbol.type().fullyQualifiedName();
    return fullyQualifiedName.startsWith("javax.swing.") || fullyQualifiedName.startsWith("java.awt.");
  }

}
