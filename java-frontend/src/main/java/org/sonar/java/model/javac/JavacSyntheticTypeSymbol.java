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
package org.sonar.java.model.javac;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;

final class JavacSyntheticTypeSymbol implements Symbol.TypeSymbol {

  private final Type type;
  private final String name;

  JavacSyntheticTypeSymbol(Type type) {
    this(type, type.name());
  }

  JavacSyntheticTypeSymbol(Type type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Symbol owner() {
    return Symbol.ROOT_PACKAGE;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public boolean isVariableSymbol() {
    return false;
  }

  @Override
  public boolean isTypeSymbol() {
    return true;
  }

  @Override
  public boolean isMethodSymbol() {
    return false;
  }

  @Override
  public boolean isPackageSymbol() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPackageVisibility() {
    return true;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return type.isUnknown();
  }

  @Override
  public SymbolMetadata metadata() {
    return Symbols.EMPTY_METADATA;
  }

  @Override
  public Symbol.TypeSymbol enclosingClass() {
    return this;
  }

  @Override
  public List<IdentifierTree> usages() {
    return List.of();
  }

  @Override
  public ClassTree declaration() {
    return null;
  }

  @Override
  public Type superClass() {
    return null;
  }

  @Override
  public List<Type> interfaces() {
    return List.of();
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    return List.of();
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    return List.of();
  }

  @Override
  public Set<Type> superTypes() {
    return Set.of();
  }

  @Override
  public Symbol.TypeSymbol outermostClass() {
    return this;
  }

  @Override
  public boolean isAnnotation() {
    return false;
  }
}
