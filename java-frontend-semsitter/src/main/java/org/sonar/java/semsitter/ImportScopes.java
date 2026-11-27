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
package org.sonar.java.semsitter;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

/** Static checks behind the unused import warning: scopes, own package and the names a type is reachable by. */
final class ImportScopes {

  private ImportScopes() {
  }

  /** The package's own types are imported implicitly, so such an import is never counted as used. */
  static boolean isOwnPackage(ImportTree importTree, String name, String currentPackage) {
    if (importTree.isStatic() || currentPackage.isEmpty()) {
      return false;
    }
    String qualifier = name.endsWith(".*") ? name.substring(0, name.length() - 2) : name.substring(0, Math.max(0, name.lastIndexOf('.')));
    return qualifier.equals(currentPackage);
  }

  /**
   * A member of another type of the same file is in scope only through an import, one of an enclosing or inherited type is not.
   * The annotations of a class are outside its scope, and a method looked up through an incomplete hierarchy may still come from an import.
   */
  static boolean isInScope(Symbol symbol, IdentifierTree identifier) {
    if (!(symbol.owner() instanceof Symbol.TypeSymbol owner) || owner.isUnknown()) {
      return true;
    }
    boolean incompleteHierarchy = false;
    boolean inScope = false;
    Tree previous = identifier;
    for (Tree tree = identifier.parent(); tree != null; previous = tree, tree = tree.parent()) {
      if (tree instanceof org.sonar.plugins.java.api.tree.ClassTree declared) {
        incompleteHierarchy |= declared.superClass() != null && declared.superClass().symbolType().isUnknown();
        Symbol.TypeSymbol enclosing = declared.symbol();
        if (!isOutsideBody(declared, previous) && (enclosing.equals(owner) || enclosing.type().isSubtypeOf(owner.type().erasure()))) {
          inScope = true;
        }
      }
    }
    return inScope && !(symbol.isMethodSymbol() && incompleteHierarchy);
  }

  /** The annotations, type parameters and supertypes of a class are not part of its body, where its members are in scope. */
  private static boolean isOutsideBody(org.sonar.plugins.java.api.tree.ClassTree declared, Tree child) {
    return child == declared.modifiers() || child == declared.typeParameters() || child == declared.superClass()
      || child == declared.superInterfaces();
  }

  static boolean denotesType(Symbol symbol, String name, IdentifierTree identifier) {
    if (symbol.isTypeSymbol()) {
      return dottedType(symbol).equals(name);
    }
    if (!(symbol.isMethodSymbol() && "<init>".equals(symbol.name()))) {
      return false;
    }
    if (symbol.owner() instanceof Symbol.TypeSymbol owner && dottedType(owner).equals(name)) {
      return true;
    }
    Type written = identifier.symbolType();
    return written != null && !written.isUnknown() && written.erasure().fullyQualifiedName().replace('$', '.').equals(name);
  }

  static boolean declaredIn(Symbol symbol, String qualifier, boolean isStatic) {
    if (symbol.isUnknown()) {
      return false;
    }
    if (isStatic && !symbol.isTypeSymbol()) {
      return symbol.owner() instanceof Symbol.TypeSymbol owner && dottedType(owner).equals(qualifier);
    }
    Symbol type = !isStatic && "<init>".equals(symbol.name()) ? symbol.owner() : symbol;
    return type.isTypeSymbol() && !type.isUnknown() && directlyIn(dottedType(type), qualifier);
  }

  /** An on-demand import brings the types declared directly in its package or type, not those of a subpackage. */
  private static boolean directlyIn(String type, String qualifier) {
    return type.startsWith(qualifier + ".") && type.indexOf('.', qualifier.length() + 1) < 0;
  }

  static String dottedType(Symbol symbol) {
    return symbol.type().fullyQualifiedName().replace('$', '.');
  }

  static String dotted(Tree tree) {
    if (tree instanceof MemberSelectExpressionTree select) {
      return dotted(select.expression()) + "." + select.identifier().name();
    }
    return ((IdentifierTree) tree).name();
  }
}
